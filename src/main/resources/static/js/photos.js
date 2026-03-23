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