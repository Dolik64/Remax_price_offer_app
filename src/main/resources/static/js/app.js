// ==================== STATE ====================

const state = {
    projectName: '',
    subject: {
        date: new Date().toISOString().split('T')[0],
        clientName: '',
        disposition: '',
        ownership: 'OV',
        area: '',
        extras: '',
        address: '',
        description: '',
        photoFilenames: []
    },
    comparables: [],
    pricing: {
        positives: [],
        negatives: [],
        recommendation: '',
        priceFrom: '',
        priceTo: '',
        startPrice: ''
    },
    // Agent defaults
    agentName: 'Martin Halgaš',
    agentTitle: 'Realitní makléř',
    agentPhone: '+420 731 502 750',
    agentEmail: 'martin.halgas@re-max.cz',
    agencyName: 'RE/MAX Anděl',
    agencyAddress: 'Ostrovského 253/3 – 150 00 Praha 5 - Smíchov',
    agentWebsite: 'www.halgasreality.cz'
};

// ==================== API ====================

const API = {
    async saveProject(data) {
        const res = await fetch('/api/projects', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return res.json();
    },

    async loadProject(name) {
        const res = await fetch(`/api/projects/${encodeURIComponent(name)}`);
        if (!res.ok) throw new Error('Projekt nenalezen');
        return res.json();
    },

    async listProjects() {
        const res = await fetch('/api/projects');
        return res.json();
    },

    async deleteProject(name) {
        const res = await fetch(`/api/projects/${encodeURIComponent(name)}`, { method: 'DELETE' });
        return res.json();
    },

    async uploadPhoto(file) {
        const formData = new FormData();
        formData.append('file', file);
        const res = await fetch('/api/photos', { method: 'POST', body: formData });
        return res.json();
    },

    async uploadPhotos(files) {
        const formData = new FormData();
        for (const f of files) formData.append('files', f);
        const res = await fetch('/api/photos/batch', { method: 'POST', body: formData });
        return res.json();
    },

    getPhotoUrl(filename) {
        return `/api/photos/${filename}`;
    },

    async exportPdf(data, preview = false) {
        const endpoint = preview ? '/api/export/pdf/preview' : '/api/export/pdf';
        const res = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return res;
    }
};

// ==================== TABS ====================

document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        // Deactivate all
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
        // Activate selected
        btn.classList.add('active');
        const tabId = btn.getAttribute('data-tab');
        document.getElementById('tab-' + tabId).classList.add('active');
        // Refresh certain tabs
        if (tabId === 'table') renderComparisonTable();
        if (tabId === 'export') updateExportStats();
    });
});

// ==================== NOTIFICATIONS ====================

function notify(message, type = 'success') {
    const el = document.createElement('div');
    el.className = `notification notification-${type}`;
    el.textContent = message;
    document.getElementById('notifications').appendChild(el);
    setTimeout(() => el.remove(), 3000);
}

// ==================== SUBJECT PHOTOS ====================

function renderSubjectPhotos() {
    const grid = document.getElementById('subjectPhotos');
    grid.innerHTML = '';

    state.subject.photoFilenames.forEach((filename, i) => {
        const item = document.createElement('div');
        item.className = 'photo-item';
        item.innerHTML = `
            <img src="${API.getPhotoUrl(filename)}" alt="Foto ${i + 1}">
            <button class="photo-remove" onclick="removeSubjectPhoto(${i})">×</button>
        `;
        grid.appendChild(item);
    });

    if (state.subject.photoFilenames.length < 6) {
        const addBtn = document.createElement('label');
        addBtn.className = 'photo-add';
        addBtn.innerHTML = `
            <span class="plus">+</span>
            <span>Foto ${state.subject.photoFilenames.length}/6</span>
            <input type="file" accept="image/*" multiple style="display:none"
                   onchange="handleSubjectPhotos(this.files)">
        `;
        grid.appendChild(addBtn);
    }
}

async function handleSubjectPhotos(files) {
    const remaining = 6 - state.subject.photoFilenames.length;
    const toUpload = Array.from(files).slice(0, remaining);

    for (const file of toUpload) {
        try {
            const result = await API.uploadPhoto(file);
            if (result.status === 'ok') {
                state.subject.photoFilenames.push(result.filename);
            } else {
                notify(result.message || 'Chyba při nahrávání', 'error');
            }
        } catch (e) {
            notify('Chyba při nahrávání fotky', 'error');
        }
    }
    renderSubjectPhotos();
}

function removeSubjectPhoto(index) {
    state.subject.photoFilenames.splice(index, 1);
    renderSubjectPhotos();
}

// ==================== COMPARABLES ====================

function newComparable() {
    return {
        id: Date.now().toString(36) + Math.random().toString(36).substr(2, 4),
        category: 'OFFER',
        disposition: '',
        area: '',
        street: '',
        city: 'Praha',
        district: '',
        price: '',
        floor: '',
        extras: '',
        condition: '',
        soldDate: '',
        soldDuration: '',
        photoFilenames: [],
        descriptionText: '',
        highlights: [],
        brokerFeedback: ''
    };
}

function addComparable() {
    state.comparables.push(newComparable());
    renderComparables();
    updateCompCount();
}

function removeComparable(id) {
    state.comparables = state.comparables.filter(c => c.id !== id);
    renderComparables();
    updateCompCount();
}

function updateCompCount() {
    document.getElementById('compCount').textContent = state.comparables.length;
}

function getComp(id) {
    return state.comparables.find(c => c.id === id);
}

function renderComparables() {
    const list = document.getElementById('comparablesList');

    if (state.comparables.length === 0) {
        list.innerHTML = `
            <div class="empty-state">
                <div class="icon">🏘️</div>
                <div class="title">Zatím žádné srovnatelné nemovitosti</div>
                <div class="desc">Klikněte na "Přidat nemovitost" pro zahájení</div>
            </div>
        `;
        return;
    }

    list.innerHTML = state.comparables.map(comp => {
        const isSold = comp.category === 'SOLD';
        const priceNum = parseInt((comp.price || '0').replace(/\D/g, ''));
        const areaNum = parseFloat(comp.area || '0');
        const pricePerM2 = areaNum > 0 ? Math.round(priceNum / areaNum) : 0;

        const headerInfo = [comp.street, comp.disposition, comp.area ? comp.area + ' m²' : '']
            .filter(Boolean).join(' • ') || 'Nová nemovitost';

        const priceInfo = priceNum > 0
            ? formatPrice(priceNum) + (pricePerM2 > 0 ? ` (${formatPrice(pricePerM2)}/m²)` : '')
            : '';

        return `
        <div class="comp-card" id="comp-${comp.id}">
            <div class="comp-header ${isSold ? 'comp-header-sold' : 'comp-header-offer'}"
                 onclick="toggleComp('${comp.id}')">
                <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
                    <span class="comp-badge ${isSold ? 'comp-badge-sold' : 'comp-badge-offer'}">
                        ${isSold ? 'Prodáno' : 'Nabídka'}
                    </span>
                    <span class="comp-title">${escHtml(headerInfo)}</span>
                    <span class="comp-subtitle">${priceInfo}</span>
                </div>
                <div style="display:flex;align-items:center;gap:8px">
                    <button class="btn btn-danger btn-small" onclick="event.stopPropagation();removeComparable('${comp.id}')">Smazat</button>
                    <span class="comp-arrow" id="arrow-${comp.id}">▾</span>
                </div>
            </div>
            <div class="comp-body" id="body-${comp.id}">
                <!-- Basic info -->
                <div class="form-grid form-grid-4">
                    <div class="form-group">
                        <label class="form-label">Dispozice</label>
                        <input type="text" class="form-input" value="${escAttr(comp.disposition)}" placeholder="4+1"
                               onchange="getComp('${comp.id}').disposition=this.value;renderComparables()">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Plocha (m²)</label>
                        <input type="text" class="form-input" value="${escAttr(comp.area)}" placeholder="82"
                               onchange="getComp('${comp.id}').area=this.value;renderComparables()">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Ulice</label>
                        <input type="text" class="form-input" value="${escAttr(comp.street)}" placeholder="Bryksova"
                               onchange="getComp('${comp.id}').street=this.value;renderComparables()">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Městská část</label>
                        <input type="text" class="form-input" value="${escAttr(comp.district)}" placeholder="Černý Most"
                               onchange="getComp('${comp.id}').district=this.value">
                    </div>
                </div>
                <div class="form-grid form-grid-4">
                    <div class="form-group">
                        <label class="form-label">Cena (Kč)</label>
                        <input type="text" class="form-input" value="${escAttr(comp.price)}" placeholder="8500000"
                               onchange="getComp('${comp.id}').price=this.value;renderComparables()">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Patro</label>
                        <input type="text" class="form-input" value="${escAttr(comp.floor)}" placeholder="6. NP"
                               onchange="getComp('${comp.id}').floor=this.value">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Stav bytu</label>
                        <input type="text" class="form-input" value="${escAttr(comp.condition)}" placeholder="Po rekonstrukci"
                               onchange="getComp('${comp.id}').condition=this.value">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Příslušenství</label>
                        <input type="text" class="form-input" value="${escAttr(comp.extras)}" placeholder="lodžie, sklep"
                               onchange="getComp('${comp.id}').extras=this.value">
                    </div>
                </div>

                <!-- Category buttons -->
                <div style="display:flex;gap:8px">
                    <button class="btn-category ${!isSold ? 'active-offer' : ''}"
                            onclick="getComp('${comp.id}').category='OFFER';renderComparables()">
                        📋 V nabídce
                    </button>
                    <button class="btn-category ${isSold ? 'active-sold' : ''}"
                            onclick="getComp('${comp.id}').category='SOLD';renderComparables()">
                        ✓ Prodáno
                    </button>
                </div>

                ${isSold ? `
                <div class="form-grid form-grid-2">
                    <div class="form-group">
                        <label class="form-label">Datum prodeje</label>
                        <input type="text" class="form-input" value="${escAttr(comp.soldDate)}" placeholder="1/2025"
                               onchange="getComp('${comp.id}').soldDate=this.value">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Doba prodeje</label>
                        <input type="text" class="form-input" value="${escAttr(comp.soldDuration)}" placeholder="2 měsíce"
                               onchange="getComp('${comp.id}').soldDuration=this.value">
                    </div>
                </div>
                ` : ''}

                <!-- Photos -->
                <div class="form-group">
                    <label class="form-label">Fotografie (max 6)</label>
                    <div class="photo-grid" id="photos-${comp.id}"></div>
                </div>

                <!-- Description -->
                <div class="form-group">
                    <label class="form-label">Popis z inzerátu</label>
                    <textarea class="form-input form-textarea" rows="5" placeholder="Vložte popis z inzerátu..."
                              onchange="getComp('${comp.id}').descriptionText=this.value">${escHtml(comp.descriptionText)}</textarea>
                </div>

                <!-- Highlights -->
                <div class="form-group">
                    <label class="form-label">Důležité body (zvýraznění)</label>
                    <div class="badge-list">
                        ${comp.highlights.map((h, i) => `
                            <span class="badge">${escHtml(h)}
                                <button onclick="removeHighlight('${comp.id}', ${i})">×</button>
                            </span>
                        `).join('')}
                    </div>
                    <div class="highlight-input-row">
                        <input type="text" class="form-input" id="hlInput-${comp.id}"
                               placeholder="Přidejte důležitý bod a stiskněte Enter..."
                               onkeydown="if(event.key==='Enter')addHighlight('${comp.id}')">
                        <button class="btn-highlight" onclick="addHighlight('${comp.id}')">+ Přidat</button>
                    </div>
                </div>

                <!-- Broker feedback -->
                <div class="form-group">
                    <label class="form-label">Zpětná vazba od makléře</label>
                    <textarea class="form-input form-textarea" rows="3"
                              placeholder="Volitelné – zpětná vazba od makléře..."
                              onchange="getComp('${comp.id}').brokerFeedback=this.value">${escHtml(comp.brokerFeedback)}</textarea>
                </div>
            </div>
        </div>
        `;
    }).join('');

    // Render photo grids for each comparable
    state.comparables.forEach(comp => renderCompPhotos(comp.id));
}

function toggleComp(id) {
    const body = document.getElementById('body-' + id);
    const arrow = document.getElementById('arrow-' + id);
    if (body.style.display === 'none') {
        body.style.display = 'flex';
        arrow.classList.add('open');
    } else {
        body.style.display = 'none';
        arrow.classList.remove('open');
    }
}

// ==================== COMPARABLE PHOTOS ====================

function renderCompPhotos(compId) {
    const comp = getComp(compId);
    const grid = document.getElementById('photos-' + compId);
    if (!grid) return;
    grid.innerHTML = '';

    comp.photoFilenames.forEach((filename, i) => {
        const item = document.createElement('div');
        item.className = 'photo-item';
        item.innerHTML = `
            <img src="${API.getPhotoUrl(filename)}" alt="Foto ${i + 1}">
            <button class="photo-remove" onclick="removeCompPhoto('${compId}', ${i})">×</button>
        `;
        grid.appendChild(item);
    });

    if (comp.photoFilenames.length < 6) {
        const addBtn = document.createElement('label');
        addBtn.className = 'photo-add';
        addBtn.innerHTML = `
            <span class="plus">+</span>
            <span>Foto ${comp.photoFilenames.length}/6</span>
            <input type="file" accept="image/*" multiple style="display:none"
                   onchange="handleCompPhotos('${compId}', this.files)">
        `;
        grid.appendChild(addBtn);
    }
}

async function handleCompPhotos(compId, files) {
    const comp = getComp(compId);
    const remaining = 6 - comp.photoFilenames.length;
    const toUpload = Array.from(files).slice(0, remaining);

    for (const file of toUpload) {
        try {
            const result = await API.uploadPhoto(file);
            if (result.status === 'ok') {
                comp.photoFilenames.push(result.filename);
            } else {
                notify(result.message || 'Chyba', 'error');
            }
        } catch (e) {
            notify('Chyba při nahrávání', 'error');
        }
    }
    renderCompPhotos(compId);
}

function removeCompPhoto(compId, index) {
    const comp = getComp(compId);
    comp.photoFilenames.splice(index, 1);
    renderCompPhotos(compId);
}

// ==================== HIGHLIGHTS ====================

function addHighlight(compId) {
    const input = document.getElementById('hlInput-' + compId);
    const value = input.value.trim();
    if (value) {
        getComp(compId).highlights.push(value);
        input.value = '';
        renderComparables();
    }
}

function removeHighlight(compId, index) {
    getComp(compId).highlights.splice(index, 1);
    renderComparables();
}

// ==================== COMPARISON TABLE ====================

function renderComparisonTable() {
    const container = document.getElementById('comparisonTable');
    const offers = state.comparables.filter(c => c.category === 'OFFER');
    const sold = state.comparables.filter(c => c.category === 'SOLD');

    if (state.comparables.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="border:none">
                Přidejte srovnatelné nemovitosti pro vygenerování tabulky.
            </div>
        `;
        return;
    }

    let html = `<table class="sta-table">
        <thead><tr>
            <th>Lokalita</th>
            <th>Prodejní cena</th>
            <th>Cena za m²</th>
            <th>Výhody / Nevýhody</th>
        </tr></thead><tbody>`;

    if (offers.length > 0) {
        html += `<tr class="section-row section-offer"><td colspan="4">Nabídka</td></tr>`;
        offers.forEach(c => { html += renderTableRow(c, false); });
    }
    if (sold.length > 0) {
        html += `<tr class="section-row section-sold"><td colspan="4">Prodáno</td></tr>`;
        sold.forEach(c => { html += renderTableRow(c, true); });
    }

    html += '</tbody></table>';
    container.innerHTML = html;
}

function renderTableRow(c, isSold) {
    const priceNum = parseInt((c.price || '0').replace(/\D/g, ''));
    const areaNum = parseFloat(c.area || '0');
    const pricePerM2 = areaNum > 0 ? Math.round(priceNum / areaNum) : 0;

    let details = [c.floor, c.extras, c.condition].filter(Boolean).join(', ');
    if (isSold && c.soldDate) {
        details += `<br><span class="cell-green">Prodáno v ${escHtml(c.soldDate)}`;
        if (c.soldDuration) details += ` za ${escHtml(c.soldDuration)}`;
        details += '</span>';
    }

    return `<tr>
        <td><span class="cell-bold">${escHtml(c.street || '–')},</span><br>
            <span class="cell-small">${escHtml(c.disposition)}, ${escHtml(c.area)} m²</span></td>
        <td class="cell-bold">${formatPrice(priceNum)}</td>
        <td class="cell-bold">${formatPrice(pricePerM2)} Kč</td>
        <td style="font-size:12px;line-height:1.5">${details}</td>
    </tr>`;
}

// ==================== EXPORT ====================

function updateExportStats() {
    const totalPhotos = state.subject.photoFilenames.length +
        state.comparables.reduce((sum, c) => sum + c.photoFilenames.length, 0);
    const offers = state.comparables.filter(c => c.category === 'OFFER').length;
    const sold = state.comparables.filter(c => c.category === 'SOLD').length;

    document.getElementById('exportStats').textContent =
        `${state.comparables.length} srovnatelných nemovitostí (${offers} v nabídce, ${sold} prodaných) • ${totalPhotos} fotek celkem`;
}

async function exportPdf() {
    try {
        const res = await API.exportPdf(state, false);
        if (!res.ok) throw new Error('Export selhal');
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `STA_${state.subject.clientName || 'export'}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        notify('PDF staženo!');
    } catch (e) {
        notify('Chyba při exportu PDF', 'error');
    }
}

async function previewPdf() {
    try {
        const res = await API.exportPdf(state, true);
        if (!res.ok) throw new Error('Náhled selhal');
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
    } catch (e) {
        notify('Chyba při náhledu PDF', 'error');
    }
}

function toggleTextPreview() {
    const el = document.getElementById('textPreview');
    if (el.style.display === 'none') {
        el.style.display = 'block';
        document.getElementById('textPreviewContent').textContent = generateTextPreview();
    } else {
        el.style.display = 'none';
    }
}

function generateTextPreview() {
    let t = `SROVNÁVACÍ TRŽNÍ ANALÝZA\n`;
    t += `Datum: ${state.subject.date}\n`;
    t += `Klient: ${state.subject.clientName}\n`;
    t += `Předmět: byt ${state.subject.disposition}, ${state.subject.ownership}, ${state.subject.area} m²`;
    if (state.subject.extras) t += ` + ${state.subject.extras}`;
    t += `\nAdresa: ${state.subject.address}\n\nPopis:\n${state.subject.description}\n\n`;

    const offers = state.comparables.filter(c => c.category === 'OFFER');
    const sold = state.comparables.filter(c => c.category === 'SOLD');

    if (offers.length > 0) {
        t += `=== AKTUÁLNĚ V NABÍDCE ===\n\n`;
        offers.forEach(c => { t += compToText(c, false); });
    }
    if (sold.length > 0) {
        t += `=== NEDÁVNO PRODANÉ ===\n\n`;
        sold.forEach(c => { t += compToText(c, true); });
    }

    t += `=== CENOVÉ DOPORUČENÍ ===\n`;
    if (state.pricing.positives?.length) t += `Klady: ${state.pricing.positives.join(', ')}\n`;
    if (state.pricing.negatives?.length) t += `Zápory: ${state.pricing.negatives.join(', ')}\n`;
    if (state.pricing.recommendation) t += `Doporučení: ${state.pricing.recommendation}\n`;
    if (state.pricing.priceFrom && state.pricing.priceTo) {
        t += `Rozmezí: ${formatPrice(parseInt(state.pricing.priceFrom))} – ${formatPrice(parseInt(state.pricing.priceTo))}\n`;
    }
    if (state.pricing.startPrice) {
        t += `Počáteční cena: ${formatPrice(parseInt(state.pricing.startPrice))}\n`;
    }

    return t;
}

function compToText(c, isSold) {
    const p = parseInt((c.price || '0').replace(/\D/g, ''));
    let t = `Byt ${c.disposition}, ${c.area} m², ul. ${c.street}, ${c.district}\n`;
    t += isSold ? `Prodáno za: ${formatPrice(p)} (${c.soldDate}, ${c.soldDuration})\n` : `Cena: ${formatPrice(p)}\n`;
    if (c.descriptionText) t += `Popis: ${c.descriptionText.substring(0, 200)}...\n`;
    if (c.highlights.length > 0) t += `Důležité: ${c.highlights.join(', ')}\n`;
    if (c.brokerFeedback) t += `Makléř: ${c.brokerFeedback}\n`;
    return t + '\n';
}

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
        // Merge loaded data into state
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

// ==================== POPULATE UI FROM STATE ====================

function populateUI() {
    // Project name
    document.getElementById('projectName').value = state.projectName || '';

    // Subject
    document.getElementById('subjectDate').value = state.subject.date || '';
    document.getElementById('subjectClient').value = state.subject.clientName || '';
    document.getElementById('subjectDisposition').value = state.subject.disposition || '';
    document.getElementById('subjectOwnership').value = state.subject.ownership || 'OV';
    document.getElementById('subjectArea').value = state.subject.area || '';
    document.getElementById('subjectExtras').value = state.subject.extras || '';
    document.getElementById('subjectAddress').value = state.subject.address || '';
    document.getElementById('subjectDescription').value = state.subject.description || '';
    renderSubjectPhotos();

    // Comparables
    renderComparables();
    updateCompCount();

    // Pricing
    const p = state.pricing;
    document.getElementById('pricingPositives').value = (p.positives || []).join('\n');
    document.getElementById('pricingNegatives').value = (p.negatives || []).join('\n');
    document.getElementById('pricingRec').value = p.recommendation || '';
    document.getElementById('pricingFrom').value = p.priceFrom || '';
    document.getElementById('pricingTo').value = p.priceTo || '';
    document.getElementById('pricingStart').value = p.startPrice || '';
}

// ==================== HELPERS ====================

function formatPrice(num) {
    if (!num || isNaN(num)) return '–';
    return num.toLocaleString('cs-CZ') + ' Kč';
}

function escHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function escAttr(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

// ==================== INIT ====================

document.addEventListener('DOMContentLoaded', () => {
    populateUI();
    refreshProjectList();
});
