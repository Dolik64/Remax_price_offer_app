// ==================== API ====================

const API = {
    // Pomocná funkce: Překládá volný JS stav do striktní podoby, kterou očekává Java backend
    mapStateToBackend(frontendState) {
        // Hluboká kopie, abychom nezničili živá data v prohlížeči
        const data = JSON.parse(JSON.stringify(frontendState));

        // 1. Předmět prodeje
        if (data.subject) {
            data.subject.areaSqm = parseInt(String(data.subject.area || '0').replace(/\D/g, '')) || 0;
            delete data.subject.area;
        }

        // 2. Srovnatelné nemovitosti
        if (data.comparables) {
            data.comparables = data.comparables.map(c => {
                // Java vyžaduje Long (číslo). Pokud máme stringové ID, nahradíme ho číslem.
                if (typeof c.id === 'string') {
                    c.id = Date.now() + Math.floor(Math.random() * 10000);
                }
                c.areaSqm = parseInt(String(c.area || '0').replace(/\D/g, '')) || 0;
                c.priceKc = parseInt(String(c.price || '0').replace(/\D/g, '')) || 0;
                delete c.area;
                delete c.price;
                return c;
            });
        }

        // 3. Cenové doporučení
        if (data.pricing) {
            // Java očekává plain String oddělený \n, JS občas posílá Array
            data.pricing.positives = Array.isArray(data.pricing.positives) ? data.pricing.positives.join('\n') : (data.pricing.positives || '');
            data.pricing.negatives = Array.isArray(data.pricing.negatives) ? data.pricing.negatives.join('\n') : (data.pricing.negatives || '');

            // Vyčištění cen od mezer a jiných znaků
            data.pricing.priceFrom = parseInt(String(data.pricing.priceFrom || '0').replace(/\D/g, '')) || 0;
            data.pricing.priceTo = parseInt(String(data.pricing.priceTo || '0').replace(/\D/g, '')) || 0;
            data.pricing.startPrice = parseInt(String(data.pricing.startPrice || '0').replace(/\D/g, '')) || 0;
        }

        return data;
    },

    // Opačná funkce: Převádí Java objekty zpět pro potřeby našeho UI
    mapBackendToState(backendData) {
        const data = JSON.parse(JSON.stringify(backendData));

        if (data.subject) {
            data.subject.area = data.subject.areaSqm || '';
        }

        if (data.comparables) {
            data.comparables = data.comparables.map(c => {
                c.area = c.areaSqm || '';
                c.price = c.priceKc || '';
                return c;
            });
        }

        if (data.pricing) {
            data.pricing.positives = data.pricing.positives ? data.pricing.positives.split('\n') : [];
            data.pricing.negatives = data.pricing.negatives ? data.pricing.negatives.split('\n') : [];
        }

        return data;
    },

    async saveProject(data) {
        const mappedData = this.mapStateToBackend(data);
        const res = await fetch('/api/projects', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(mappedData)
        });
        return res.json();
    },

    async loadProject(name) {
        const res = await fetch(`/api/projects/${encodeURIComponent(name)}`);
        if (!res.ok) throw new Error('Projekt nenalezen');
        const data = await res.json();
        return this.mapBackendToState(data);
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

    getPhotoUrl(filename) {
        return `/api/photos/${filename}`;
    },

    async exportPdf(data, preview = false) {
        const mappedData = this.mapStateToBackend(data);
        const endpoint = preview ? '/api/export/pdf/preview' : '/api/export/pdf';
        return fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(mappedData)
        });
    },

    async exportDocx(data) {
        const mappedData = this.mapStateToBackend(data);
        return fetch('/api/export/docx', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(mappedData)
        });
    }
};