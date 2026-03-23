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
    agentName: 'Martin Halgaš',
    agentTitle: 'Realitní makléř',
    agentPhone: '+420 731 502 750',
    agentEmail: 'martin.halgas@re-max.cz',
    agencyName: 'RE/MAX Anděl',
    agencyAddress: 'Ostrovského 253/3 – 150 00 Praha 5 - Smíchov',
    agentWebsite: 'www.halgasreality.cz'
};

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

// ==================== NOTIFICATIONS ====================

function notify(message, type = 'success') {
    const el = document.createElement('div');
    el.className = `notification notification-${type}`;
    el.textContent = message;
    document.getElementById('notifications').appendChild(el);
    setTimeout(() => el.remove(), 3000);
}