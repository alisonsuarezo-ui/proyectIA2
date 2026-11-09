const API_BASE = '/api';
let currentContracts = [];
let contractTypes = [];

document.addEventListener('DOMContentLoaded', () => {
    setupNavigation();
    setupAuth();
    setupForms();
});

function setupAuth() {
    const savedUser = sessionStorage.getItem('legaldocs_user');
    if (savedUser) {
        enterApp(JSON.parse(savedUser));
    }

    document.getElementById('login-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        const errBox = document.getElementById('login-error');
        errBox.classList.add('hidden');

        try {
            const res = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Error de autenticación');

            sessionStorage.setItem('legaldocs_user', JSON.stringify(data));
            enterApp(data);
        } catch (err) {
            errBox.textContent = err.message;
            errBox.classList.remove('hidden');
        }
    });

    document.getElementById('logout-btn').addEventListener('click', () => {
        sessionStorage.removeItem('legaldocs_user');
        document.getElementById('app-container').classList.add('hidden');
        document.getElementById('login-screen').classList.remove('hidden');
    });
}

async function enterApp(user) {
    document.getElementById('user-display').textContent = `${user.fullName} (${user.role})`;
    document.getElementById('login-screen').classList.add('hidden');
    document.getElementById('app-container').classList.remove('hidden');
    await loadContractTypes();
    await loadDashboard();
}

function setupNavigation() {
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const view = btn.getAttribute('data-view');
            switchView(view);
        });
    });
}

function switchView(viewName) {
    document.querySelectorAll('.view-section').forEach(s => s.classList.add('hidden'));
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));

    const activeBtn = document.querySelector(`.nav-btn[data-view="${viewName}"]`);
    if (activeBtn) activeBtn.classList.add('active');

    document.getElementById(`view-${viewName}`).classList.remove('hidden');

    if (viewName === 'dashboard') loadDashboard();
    if (viewName === 'contracts') loadContracts('');
    if (viewName === 'rag') populateRagContractsDropdown();
    if (viewName === 'obligations') loadAllObligations();
    if (viewName === 'alerts') loadAlertsCenter();
}

async function loadDashboard() {
    try {
        const res = await fetch(`${API_BASE}/dashboard`);
        const data = await res.json();

        document.getElementById('stat-total').textContent = data.totalContracts;
        document.getElementById('stat-active').textContent = data.activeContracts;
        document.getElementById('stat-expiring').textContent = data.expiringSoonContracts;
        document.getElementById('stat-expired').textContent = data.expiredContracts;
        document.getElementById('stat-obligations').textContent = data.pendingObligationsCount;

        const expTbody = document.getElementById('dash-expiring-tbody');
        expTbody.innerHTML = data.upcomingExpirations.length
            ? data.upcomingExpirations.map(c => `
                <tr>
                    <td><strong>${c.code}</strong></td>
                    <td>${c.title}</td>
                    <td>${c.endDate}</td>
                    <td><span class="badge badge-${c.status}">${c.status}</span></td>
                </tr>`).join('')
            : '<tr><td colspan="4" class="text-muted">No hay contratos próximos a vencer en 30 días.</td></tr>';

        const alertsTbody = document.getElementById('dash-alerts-tbody');
        alertsTbody.innerHTML = data.recentAlerts.length
            ? data.recentAlerts.map(a => `
                <tr>
                    <td>${a.contract ? a.contract.code : 'N/A'}</td>
                    <td><span class="badge badge-EXPIRING_SOON">${a.alertType}</span></td>
                    <td>${a.recipientEmail}</td>
                    <td>${a.sentAt ? a.sentAt.replace('T', ' ').substring(0, 16) : ''}</td>
                </tr>`).join('')
            : '<tr><td colspan="4" class="text-muted">Sin alertas registradas aún.</td></tr>';

        window.cachedPendingObligations = data.pendingObligations || [];
    } catch (e) {
        console.error('Error cargando dashboard:', e);
    }
}

async function loadContractTypes() {
    const res = await fetch(`${API_BASE}/contracts/types`);
    contractTypes = await res.json();
    const select = document.getElementById('contract-type');
    select.innerHTML = contractTypes.map(t => `<option value="${t.id}">${t.name}</option>`).join('');
}

async function loadContracts(status = '') {
    const url = status ? `${API_BASE}/contracts?status=${status}` : `${API_BASE}/contracts`;
    const res = await fetch(url);
    currentContracts = await res.json();

    const tbody = document.getElementById('contracts-tbody');
    tbody.innerHTML = currentContracts.length
        ? currentContracts.map(c => `
            <tr>
                <td><strong>${c.code}</strong></td>
                <td>${c.title}</td>
                <td>${c.counterparty}</td>
                <td>${c.contractType ? c.contractType.name : 'Sin clasificar'}</td>
                <td>${c.startDate}</td>
                <td>${c.endDate}</td>
                <td><span class="badge badge-${c.status}">${c.status}</span></td>
                <td>
                    <button class="btn btn-outline btn-sm" onclick="openContractDetail(${c.id})">Ver / PDF / IA</button>
                    <button class="btn btn-outline btn-sm" onclick="deleteContract(${c.id})">🗑</button>
                </td>
            </tr>`).join('')
        : '<tr><td colspan="8" class="text-muted">No se encontraron contratos registrados.</td></tr>';
}

function filterContracts(status, chipEl) {
    document.querySelectorAll('.filter-bar .chip').forEach(c => c.classList.remove('active'));
    chipEl.classList.add('active');
    loadContracts(status);
}

function openContractModal() {
    document.getElementById('contract-form').reset();
    document.getElementById('contract-id').value = '';
    document.getElementById('contract-modal').classList.remove('hidden');
}

function closeContractModal() {
    document.getElementById('contract-modal').classList.add('hidden');
}

async function openContractDetail(id) {
    const res = await fetch(`${API_BASE}/contracts/${id}`);
    const c = await res.json();

    document.querySelectorAll('.view-section').forEach(s => s.classList.add('hidden'));
    document.getElementById('view-contract-detail').classList.remove('hidden');

    document.getElementById('detail-title').textContent = `${c.code} — ${c.title}`;
    document.getElementById('detail-subtitle').textContent = `Contraparte: ${c.counterparty} | Tipo: ${c.contractType ? c.contractType.name : 'Pendiente IA'}`;
    document.getElementById('upload-contract-id').value = c.id;

    document.getElementById('detail-info').innerHTML = `
        <p><strong>Vigencia:</strong> ${c.startDate} al ${c.endDate} (<span class="badge badge-${c.status}">${c.status}</span>)</p>
        <p style="margin-top:0.4rem;"><strong>Observaciones:</strong> ${c.observations || 'Ninguna'}</p>
    `;

    document.getElementById('detail-summary').textContent = c.summary || 'Sube un PDF para generar el resumen ejecutivo fundamentado.';

    const docList = document.getElementById('detail-documents-list');
    docList.innerHTML = (c.documents && c.documents.length)
        ? c.documents.map(d => `<li>📄 <strong>v${d.version}</strong> - ${d.fileName} (${d.pageCount} págs.)</li>`).join('')
        : '<li class="text-muted">Sin documentos PDF cargados.</li>';

    const clausesGrid = document.getElementById('detail-clauses-grid');
    clausesGrid.innerHTML = (c.clauses && c.clauses.length)
        ? c.clauses.map(cl => `
            <div class="clause-card">
                <div class="clause-header">
                    <span>${cl.category}</span>
                    <span>Pág. ${cl.pageNumber || 1}</span>
                </div>
                <h4>${cl.title}</h4>
                <p>${cl.content}</p>
            </div>`).join('')
        : '<p class="text-muted">No hay cláusulas extraídas todavía.</p>';

    const obTbody = document.getElementById('detail-obligations-tbody');
    obTbody.innerHTML = (c.obligations && c.obligations.length)
        ? c.obligations.map(ob => `
            <tr>
                <td>${ob.description}</td>
                <td>${ob.dueDate}</td>
                <td>${ob.responsibleEmail || 'admin@legaldocs.com'}</td>
                <td><span class="badge badge-${ob.status}">${ob.status}</span></td>
                <td>
                    ${ob.status !== 'COMPLETED'
                        ? `<button class="btn btn-outline btn-sm" onclick="completeObligation(${ob.id}, ${c.id})">✓ Cumplida</button>`
                        : 'Completada'}
                </td>
            </tr>`).join('')
        : '<tr><td colspan="5" class="text-muted">Sin obligaciones registradas.</td></tr>';
}

async function deleteContract(id) {
    if (!confirm('¿Eliminar este contrato y todos sus vectores en Qdrant?')) return;
    await fetch(`${API_BASE}/contracts/${id}`, { method: 'DELETE' });
    loadContracts('');
}

async function completeObligation(obligationId, contractId = null) {
    await fetch(`${API_BASE}/contracts/obligations/${obligationId}/status?status=COMPLETED`, { method: 'PATCH' });
    if (contractId) {
        openContractDetail(contractId);
    } else {
        loadAllObligations();
    }
}

function setupForms() {
    document.getElementById('contract-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            code: document.getElementById('contract-code').value,
            title: document.getElementById('contract-title').value,
            counterparty: document.getElementById('contract-counterparty').value,
            startDate: document.getElementById('contract-start').value,
            endDate: document.getElementById('contract-end').value,
            contractTypeId: Number(document.getElementById('contract-type').value) || null,
            observations: document.getElementById('contract-observations').value
        };

        const res = await fetch(`${API_BASE}/contracts`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            closeContractModal();
            loadContracts('');
        } else {
            const err = await res.json();
            alert(err.error || 'Error al guardar el contrato');
        }
    });

    document.getElementById('pdf-upload-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const contractId = document.getElementById('upload-contract-id').value;
        const fileInput = document.getElementById('pdf-file-input');
        const statusBox = document.getElementById('upload-status');
        const btn = document.getElementById('btn-upload-pdf');

        if (!fileInput.files.length) return;

        const formData = new FormData();
        formData.append('file', fileInput.files[0]);

        btn.disabled = true;
        statusBox.textContent = '⏳ Extrayendo texto con PDFBox, analizando cláusulas con IA y generando embeddings en Qdrant...';
        statusBox.classList.remove('hidden');

        try {
            const res = await fetch(`${API_BASE}/contracts/${contractId}/upload-pdf`, {
                method: 'POST',
                body: formData
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Error procesando PDF');

            statusBox.textContent = '✅ PDF procesado e indexado correctamente en PostgreSQL y Qdrant.';
            fileInput.value = '';
            await openContractDetail(contractId);
        } catch (err) {
            statusBox.textContent = '❌ ' + err.message;
        } finally {
            btn.disabled = false;
        }
    });

    document.getElementById('rag-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const question = document.getElementById('rag-question').value;
        const contractIdVal = document.getElementById('rag-contract-select').value;
        const categoryVal = document.getElementById('rag-category-select').value;
        const btn = document.getElementById('btn-rag-ask');

        btn.disabled = true;
        btn.textContent = 'Consultando Qdrant + LLM...';

        try {
            const res = await fetch(`${API_BASE}/rag/ask`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    question,
                    contractId: contractIdVal ? Number(contractIdVal) : null,
                    clauseCategory: categoryVal || null
                })
            });
            const data = await res.json();

            document.getElementById('rag-result-container').classList.remove('hidden');
            document.getElementById('rag-answer-box').textContent = data.answer;

            const refGrid = document.getElementById('rag-references-grid');
            refGrid.innerHTML = (data.references && data.references.length)
                ? data.references.map(r => `
                    <div class="clause-card">
                        <div class="clause-header">
                            <span>${r.contractCode} (${r.clauseCategory})</span>
                            <span>Pág. ${r.pageNumber}</span>
                        </div>
                        <h4>${r.clauseTitle} — <small>${r.fileName}</small></h4>
                        <p>${r.excerpt}</p>
                    </div>`).join('')
                : '<p class="text-muted">No se recuperaron fragmentos con similitud suficiente en Qdrant.</p>';
        } catch (err) {
            alert('Error en consulta RAG: ' + err.message);
        } finally {
            btn.disabled = false;
            btn.textContent = 'Consultar Documentos';
        }
    });
}

function setQuickQuestion(q) {
    document.getElementById('rag-question').value = q;
}

async function populateRagContractsDropdown() {
    const res = await fetch(`${API_BASE}/contracts`);
    const list = await res.json();
    const select = document.getElementById('rag-contract-select');
    select.innerHTML = '<option value="">Todos los contratos registrados</option>' +
        list.map(c => `<option value="${c.id}">${c.code} — ${c.title}</option>`).join('');
}

async function loadAllObligations() {
    await loadDashboard();
    const list = window.cachedPendingObligations || [];
    const tbody = document.getElementById('all-obligations-tbody');
    tbody.innerHTML = list.length
        ? list.map(ob => `
            <tr>
                <td>${ob.description}</td>
                <td>${ob.dueDate}</td>
                <td>${ob.responsibleEmail || 'admin@legaldocs.com'}</td>
                <td><span class="badge badge-${ob.status}">${ob.status}</span></td>
                <td><button class="btn btn-outline btn-sm" onclick="completeObligation(${ob.id})">✓ Marcar Cumplida</button></td>
            </tr>`).join('')
        : '<tr><td colspan="5" class="text-muted">No hay obligaciones pendientes actualmente.</td></tr>';
}

async function loadAlertsCenter() {
    const [pendingRes, sentRes] = await Promise.all([
        fetch(`${API_BASE}/automation/pending-notifications`),
        fetch(`${API_BASE}/automation/alerts`)
    ]);
    const pendingList = await pendingRes.json();
    const sentList = await sentRes.json();

    document.getElementById('pending-notifications-tbody').innerHTML = pendingList.length
        ? pendingList.map(p => `
            <tr>
                <td><strong>${p.contractCode}</strong></td>
                <td><span class="badge badge-EXPIRING_SOON">${p.alertType}</span></td>
                <td>${p.daysRemaining} días</td>
                <td>${p.recipientEmail}</td>
                <td>${p.messageDetail}</td>
            </tr>`).join('')
        : '<tr><td colspan="5" class="text-muted">No hay notificaciones pendientes por enviar.</td></tr>';

    document.getElementById('sent-alerts-tbody').innerHTML = sentList.length
        ? sentList.map(a => `
            <tr>
                <td><strong>${a.contract ? a.contract.code : 'N/A'}</strong></td>
                <td><span class="badge badge-SENT">${a.alertType}</span></td>
                <td>${a.recipientEmail}</td>
                <td>${a.sentAt ? a.sentAt.replace('T', ' ').substring(0, 16) : ''}</td>
                <td><span class="badge badge-${a.status}">${a.status}</span></td>
            </tr>`).join('')
        : '<tr><td colspan="5" class="text-muted">Aún no se han registrado alertas enviadas por n8n.</td></tr>';
}
