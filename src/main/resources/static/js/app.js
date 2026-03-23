// ==================== TABS ====================

document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
        btn.classList.add('active');
        const tabId = btn.getAttribute('data-tab');
        document.getElementById('tab-' + tabId).classList.add('active');
        if (tabId === 'table') renderComparisonTable();
        if (tabId === 'export') updateExportStats();
    });
});

// ==================== PROJECT SAVE/LOAD ====================

async function saveProject() {
    const name = document.getElementById('projectName').value.trim();
    if (!name) {
        notify('Zadejte název projektu', 'error');
        document.getElementById('projectName').focus();
        return;
    }
    state.projectName = name;
    try {
        await API.saveProject(state);
        notify('Projekt uložen!');
        refreshProjectList();
    } catch (e) {
        notify('Chyba při ukládání', 'error');
    }
}

async function refreshProjectList() {
    try {
        const projects = await API.listProjects();
        const select = document.getElementById('projectSelect');
        select.innerHTML = '<option value="">— Načíst projekt —</option>';
        projects.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p;
            opt.textContent = p;
            select.appendChild(opt);
        });
    } catch (e) {
        console.error('Failed to list projects', e);
    }
}

async function loadSelectedProject() {
    const name = document.getElementById('projectSelect').value;
    if (!name) return;
    try {
        const data = await API.loadProject(name);
        Object.assign(state, data);
        populateUI();
        notify('Projekt načten!');
    } catch (e) {
        notify('Chyba při načítání projektu', 'error');
    }
}

async function deleteSelectedProject() {
    const name = document.getElementById('projectSelect').value;
    if (!name) return;
    if (!confirm(`Smazat projekt "${name}"?`)) return;
    try {
        await API.deleteProject(name);
        notify('Projekt smazán');
        refreshProjectList();
    } catch (e) {
        notify('Chyba při mazání', 'error');
    }
}

function resetAll() {
    if (!confirm('Opravdu chcete smazat všechna data?')) return;
    state.projectName = '';
    state.subject = {
        date: new Date().toISOString().split('T')[0],
        clientName: '', disposition: '', ownership: 'OV',
        area: '', extras: '', address: '', description: '', photoFilenames: []
    };
    state.comparables = [];
    state.pricing = { positives: [], negatives: [], recommendation: '', priceFrom: '', priceTo: '', startPrice: '' };
    populateUI();
    notify('Data resetována');
}

// ==================== POPULATE UI ====================

function populateUI() {
    document.getElementById('projectName').value = state.projectName || '';

    document.getElementById('subjectDate').value = state.subject.date || '';
    document.getElementById('subjectClient').value = state.subject.clientName || '';
    document.getElementById('subjectDisposition').value = state.subject.disposition || '';
    document.getElementById('subjectOwnership').value = state.subject.ownership || 'OV';
    document.getElementById('subjectArea').value = state.subject.area || '';
    document.getElementById('subjectExtras').value = state.subject.extras || '';
    document.getElementById('subjectAddress').value = state.subject.address || '';
    document.getElementById('subjectDescription').value = state.subject.description || '';
    renderSubjectPhotos();

    renderComparables();
    updateCompCount();

    const p = state.pricing;
    document.getElementById('pricingPositives').value = (p.positives || []).join('\n');
    document.getElementById('pricingNegatives').value = (p.negatives || []).join('\n');
    document.getElementById('pricingRec').value = p.recommendation || '';
    document.getElementById('pricingFrom').value = p.priceFrom || '';
    document.getElementById('pricingTo').value = p.priceTo || '';
    document.getElementById('pricingStart').value = p.startPrice || '';
}

// ==================== INIT ====================

document.addEventListener('DOMContentLoaded', () => {
    populateUI();
    refreshProjectList();
});