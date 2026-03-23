// ==================== COMPARABLES ====================

function newComparable() {
    return {
        id: Date.now().toString(36) + Math.random().toString(36).substr(2, 4),
        category: 'OFFER',
        disposition: '', area: '', street: '', city: 'Praha', district: '',
        price: '', floor: '', extras: '', condition: '',
        soldDate: '', soldDuration: '',
        photoFilenames: [], descriptionText: '', highlights: [], brokerFeedback: ''
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

// ==================== UPDATE FIELD (BEZ ZTRÁTY FOCUSU) ====================

function updateCompField(compId, field, value) {
    const comp = getComp(compId);
    comp[field] = value;

    const titleEl = document.getElementById('title-' + compId);
    const subtitleEl = document.getElementById('subtitle-' + compId);

    if (titleEl && subtitleEl) {
        const headerInfo = [comp.street, comp.disposition, comp.area ? comp.area + ' m²' : '']
            .filter(Boolean).join(' • ') || 'Nová nemovitost';

        const priceNum = parseInt((comp.price || '0').replace(/\D/g, ''));
        const areaNum = parseFloat(comp.area || '0');
        const pricePerM2 = areaNum > 0 ? Math.round(priceNum / areaNum) : 0;
        const priceInfo = priceNum > 0
            ? formatPrice(priceNum) + (pricePerM2 > 0 ? ` (${formatPrice(pricePerM2)}/m²)` : '') : '';

        titleEl.textContent = headerInfo;
        subtitleEl.textContent = priceInfo;
    }
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

// ==================== RENDER COMPARABLES ====================

function renderComparables() {
    const list = document.getElementById('comparablesList');

    if (state.comparables.length === 0) {
        list.innerHTML = `
            <div class="empty-state">
                <div class="icon">🏘️</div>
                <div class="title">Zatím žádné srovnatelné nemovitosti</div>
                <div class="desc">Klikněte na "Přidat nemovitost" pro zahájení</div>
            </div>`;
        return;
    }

    list.innerHTML = state.comparables.map(comp => renderCompCard(comp)).join('');
    state.comparables.forEach(comp => renderCompPhotos(comp.id));
}

function renderCompCard(comp) {
    const isSold = comp.category === 'SOLD';
    const priceNum = parseInt((comp.price || '0').replace(/\D/g, ''));
    const areaNum = parseFloat(comp.area || '0');
    const pricePerM2 = areaNum > 0 ? Math.round(priceNum / areaNum) : 0;

    const headerInfo = [comp.street, comp.disposition, comp.area ? comp.area + ' m²' : '']
        .filter(Boolean).join(' • ') || 'Nová nemovitost';
    const priceInfo = priceNum > 0
        ? formatPrice(priceNum) + (pricePerM2 > 0 ? ` (${formatPrice(pricePerM2)}/m²)` : '') : '';

    return `
    <div class="comp-card" id="comp-${comp.id}">
        <div class="comp-header ${isSold ? 'comp-header-sold' : 'comp-header-offer'}"
             onclick="toggleComp('${comp.id}')">
            <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
                <span class="comp-badge ${isSold ? 'comp-badge-sold' : 'comp-badge-offer'}">
                    ${isSold ? 'Prodáno' : 'Nabídka'}
                </span>
                <span class="comp-title" id="title-${comp.id}">${escHtml(headerInfo)}</span>
                <span class="comp-subtitle" id="subtitle-${comp.id}">${escHtml(priceInfo)}</span>
            </div>
            <div style="display:flex;align-items:center;gap:8px">
                <button class="btn btn-danger btn-small" onclick="event.stopPropagation();removeComparable('${comp.id}')">Smazat</button>
                <span class="comp-arrow" id="arrow-${comp.id}">▾</span>
            </div>
        </div>
        <div class="comp-body" id="body-${comp.id}">
            <div class="form-grid form-grid-2">
                <div class="form-group"><label class="form-label">Dispozice</label>
                    <input type="text" class="form-input" value="${escAttr(comp.disposition)}" placeholder="4+1"
                           onchange="updateCompField('${comp.id}', 'disposition', this.value)"></div>
                <div class="form-group"><label class="form-label">Plocha (m²)</label>
                    <input type="text" class="form-input" value="${escAttr(comp.area)}" placeholder="82"
                           onchange="updateCompField('${comp.id}', 'area', this.value)"></div>
            </div>
            <div class="form-grid form-grid-3">
                <div class="form-group"><label class="form-label">Ulice</label>
                    <input type="text" class="form-input" value="${escAttr(comp.street)}" placeholder="Bryksova"
                           onchange="updateCompField('${comp.id}', 'street', this.value)"></div>
                <div class="form-group"><label class="form-label">Město</label>
                    <input type="text" class="form-input" value="${escAttr(comp.city)}" placeholder="Praha"
                           onchange="updateCompField('${comp.id}', 'city', this.value)"></div>
                <div class="form-group"><label class="form-label">Městská část</label>
                    <input type="text" class="form-input" value="${escAttr(comp.district)}" placeholder="Černý Most"
                           onchange="updateCompField('${comp.id}', 'district', this.value)"></div>
            </div>
            <div class="form-grid form-grid-4">
                <div class="form-group"><label class="form-label">Cena (Kč)</label>
                    <input type="text" class="form-input" value="${escAttr(comp.price)}" placeholder="8500000"
                           onchange="updateCompField('${comp.id}', 'price', this.value)"></div>
                <div class="form-group"><label class="form-label">Patro</label>
                    <input type="text" class="form-input" value="${escAttr(comp.floor)}" placeholder="6. NP"
                           onchange="updateCompField('${comp.id}', 'floor', this.value)"></div>
                <div class="form-group"><label class="form-label">Stav bytu</label>
                    <input type="text" class="form-input" value="${escAttr(comp.condition)}" placeholder="Po rekonstrukci"
                           onchange="updateCompField('${comp.id}', 'condition', this.value)"></div>
                <div class="form-group"><label class="form-label">Příslušenství</label>
                    <input type="text" class="form-input" value="${escAttr(comp.extras)}" placeholder="lodžie, sklep"
                           onchange="updateCompField('${comp.id}', 'extras', this.value)"></div>
            </div>
            <div style="display:flex;gap:8px">
                <button class="btn-category ${!isSold ? 'active-offer' : ''}"
                        onclick="getComp('${comp.id}').category='OFFER';renderComparables()">📋 V nabídce</button>
                <button class="btn-category ${isSold ? 'active-sold' : ''}"
                        onclick="getComp('${comp.id}').category='SOLD';renderComparables()">✓ Prodáno</button>
            </div>
            ${isSold ? `
            <div class="form-grid form-grid-2">
                <div class="form-group"><label class="form-label">Datum prodeje</label>
                    <input type="text" class="form-input" value="${escAttr(comp.soldDate)}" placeholder="1/2025"
                           onchange="updateCompField('${comp.id}', 'soldDate', this.value)"></div>
                <div class="form-group"><label class="form-label">Doba prodeje</label>
                    <input type="text" class="form-input" value="${escAttr(comp.soldDuration)}" placeholder="2 měsíce"
                           onchange="updateCompField('${comp.id}', 'soldDuration', this.value)"></div>
            </div>` : ''}
            <div class="form-group"><label class="form-label">Fotografie (max 6)</label>
                <div class="photo-grid" id="photos-${comp.id}"></div></div>
            <div class="form-group"><label class="form-label">Popis z inzerátu</label>
                <textarea class="form-input form-textarea" rows="5" placeholder="Vložte popis z inzerátu..."
                          onchange="updateCompField('${comp.id}', 'descriptionText', this.value)">${escHtml(comp.descriptionText)}</textarea></div>
            <div class="form-group"><label class="form-label">Důležité body (zvýraznění)</label>
                <div class="badge-list">
                    ${comp.highlights.map((h, i) => `<span class="badge">${escHtml(h)}<button onclick="removeHighlight('${comp.id}', i)">×</button></span>`).join('')}
                </div>
                <div class="highlight-input-row">
                    <input type="text" class="form-input" id="hlInput-${comp.id}"
                           placeholder="Přidejte důležitý bod a stiskněte Enter..."
                           onkeydown="if(event.key==='Enter')addHighlight('${comp.id}')">
                    <button class="btn-highlight" onclick="addHighlight('${comp.id}')">+ Přidat</button>
                </div></div>
            <div class="form-group"><label class="form-label">Zpětná vazba od makléře</label>
                <textarea class="form-input form-textarea" rows="3" placeholder="Volitelné – zpětná vazba od makléře..."
                          onchange="updateCompField('${comp.id}', 'brokerFeedback', this.value)">${escHtml(comp.brokerFeedback)}</textarea></div>
        </div>
    </div>`;
}

// ==================== COMPARISON TABLE ====================

function renderComparisonTable() {
    const container = document.getElementById('comparisonTable');
    const offers = state.comparables.filter(c => c.category === 'OFFER');
    const sold = state.comparables.filter(c => c.category === 'SOLD');

    if (state.comparables.length === 0) {
        container.innerHTML = '<div class="empty-state" style="border:none">Přidejte srovnatelné nemovitosti.</div>';
        return;
    }

    let html = `<table class="sta-table"><thead><tr>
        <th>Lokalita</th><th>Prodejní cena</th><th>Cena za m²</th><th>Výhody / Nevýhody</th>
    </tr></thead><tbody>`;

    if (offers.length > 0) {
        html += '<tr class="section-row section-offer"><td colspan="4">Nabídka</td></tr>';
        offers.forEach(c => { html += renderTableRow(c, false); });
    }
    if (sold.length > 0) {
        html += '<tr class="section-row section-sold"><td colspan="4">Prodáno</td></tr>';
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