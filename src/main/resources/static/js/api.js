// ==================== API ====================

const API = {
    mapStateToBackend(frontendState) {
        const data = JSON.parse(JSON.stringify(frontendState));

        if (data.subject) {
            data.subject.areaSqm = parseInt(String(data.subject.area || '0').replace(/\D/g, '')) || 0;
            delete data.subject.area;
        }

        if (data.comparables) {
            data.comparables = data.comparables.map(c => {
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

        if (data.pricing) {
            data.pricing.positives = Array.isArray(data.pricing.positives) ? data.pricing.positives.join('\n') : (data.pricing.positives || '');
            data.pricing.negatives = Array.isArray(data.pricing.negatives) ? data.pricing.negatives.join('\n') : (data.pricing.negatives || '');

            data.pricing.priceFrom = parseInt(String(data.pricing.priceFrom || '0').replace(/\D/g, '')) || 0;
            data.pricing.priceTo = parseInt(String(data.pricing.priceTo || '0').replace(/\D/g, '')) || 0;
            data.pricing.startPrice = parseInt(String(data.pricing.startPrice || '0').replace(/\D/g, '')) || 0;
        }

        return data;
    },

    mapBackendToState(backendData) {
        const data = JSON.parse(JSON.stringify(backendData));

        if (data.subject) {
            // Oprava: Bezpečné přetypování na String, aby vstupní políčka nezlobila
            data.subject.area = String(data.subject.areaSqm || '');
        }

        if (data.comparables) {
            data.comparables = data.comparables.map(c => {
                // Oprava: Bezpečné přetypování na String
                c.area = String(c.areaSqm || '');
                c.price = String(c.priceKc || '');
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