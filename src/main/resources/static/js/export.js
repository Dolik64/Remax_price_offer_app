// ==================== EXPORT ====================

function updateExportStats() {
    const totalPhotos = state.subject.photoFilenames.length +
        state.comparables.reduce((sum, c) => sum + c.photoFilenames.length, 0);
    const offers = state.comparables.filter(c => c.category === 'OFFER').length;
    const sold = state.comparables.filter(c => c.category === 'SOLD').length;

    document.getElementById('exportStats').textContent =
        `${state.comparables.length} srovnatelných nemovitostí (${offers} v nabídce, ${sold} prodaných) • ${totalPhotos} fotek celkem`;
}

async function exportDocx() {
    try {
        const res = await API.exportDocx(state);
        if (!res.ok) throw new Error('Export selhal');
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `STA_${state.subject.clientName || 'export'}.docx`;
        a.click();
        URL.revokeObjectURL(url);
        notify('DOCX staženo!');
    } catch (e) {
        notify('Chyba při exportu DOCX', 'error');
    }
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

    const pos = Array.isArray(state.pricing.positives) ? state.pricing.positives : [];
    const neg = Array.isArray(state.pricing.negatives) ? state.pricing.negatives : [];

    if (pos.length) t += `Klady: ${pos.join(', ')}\n`;
    if (neg.length) t += `Zápory: ${neg.join(', ')}\n`;
    if (state.pricing.recommendation) t += `Doporučení: ${state.pricing.recommendation}\n`;

    // Opravený regex na /\D/g
    if (state.pricing.priceFrom && state.pricing.priceTo) {
        t += `Rozmezí: ${formatPrice(parseInt(String(state.pricing.priceFrom).replace(/\D/g, '')))} – ${formatPrice(parseInt(String(state.pricing.priceTo).replace(/\D/g, '')))}\n`;
    }
    if (state.pricing.startPrice) {
        t += `Počáteční cena: ${formatPrice(parseInt(String(state.pricing.startPrice).replace(/\D/g, '')))}\n`;
    }
    return t;
}

function compToText(c, isSold) {
    const p = parseInt((c.price || '0').toString().replace(/\D/g, ''));
    let t = `Byt ${c.disposition}, ${c.area} m², ul. ${c.street}, ${c.district}\n`;
    t += isSold ? `Prodáno za: ${formatPrice(p)} (${c.soldDate}, ${c.soldDuration})\n` : `Cena: ${formatPrice(p)}\n`;
    if (c.descriptionText) t += `Popis: ${c.descriptionText.substring(0, 200)}...\n`;
    if (c.highlights.length > 0) t += `Důležité: ${c.highlights.join(', ')}\n`;
    if (c.brokerFeedback) t += `Makléř: ${c.brokerFeedback}\n`;
    return t + '\n';
}