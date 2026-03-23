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

    getPhotoUrl(filename) {
        return `/api/photos/${filename}`;
    },

    async exportPdf(data, preview = false) {
        const endpoint = preview ? '/api/export/pdf/preview' : '/api/export/pdf';
        return fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
    }
};