// ==================== POMOCNÁ FUNKCE PRO HEIC/HEIF ====================

async function processPhotosBeforeUpload(files, maxAllowed) {
    const toUpload = [];
    const filesArray = Array.from(files).slice(0, maxAllowed);

    for (const file of filesArray) {
        // Rozšířená kontrola pro .heic i .heif
        const nameLower = file.name.toLowerCase();
        const isHeicOrHeif = nameLower.endsWith('.heic') ||
            nameLower.endsWith('.heif') ||
            file.type === 'image/heic' ||
            file.type === 'image/heif';

        if (isHeicOrHeif) {
            try {
                notify('Konvertuji Apple formát (HEIC/HEIF)...', 'success');
                const convertedBlob = await heic2any({
                    blob: file,
                    toType: 'image/jpeg',
                    quality: 0.8
                });

                const blob = Array.isArray(convertedBlob) ? convertedBlob[0] : convertedBlob;

                // Přejmenujeme koncovku bez ohledu na to, jestli to byl heic nebo heif
                const newName = file.name.replace(/\.(heic|heif)$/i, '.jpg');
                const newFile = new File([blob], newName, { type: 'image/jpeg' });
                toUpload.push(newFile);

            } catch (e) {
                console.error("Chyba při převodu HEIC/HEIF:", e);
                notify(`Nepodařilo se převést fotku ${file.name}`, 'error');
            }
        } else {
            toUpload.push(file);
        }
    }

    return toUpload;
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
        // Přidáno .heif do accept atributu
        addBtn.innerHTML = `
            <span class="plus">+</span>
            <span>Foto ${state.subject.photoFilenames.length}/6</span>
            <input type="file" accept="image/*,.heic,.heif" multiple style="display:none"
                   onchange="handleSubjectPhotos(this.files)">
        `;
        grid.appendChild(addBtn);
    }
}

async function handleSubjectPhotos(files) {
    const remaining = 6 - state.subject.photoFilenames.length;

    const processedFiles = await processPhotosBeforeUpload(files, remaining);

    for (const file of processedFiles) {
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
        // Přidáno .heif do accept atributu
        addBtn.innerHTML = `
            <span class="plus">+</span>
            <span>Foto ${comp.photoFilenames.length}/6</span>
            <input type="file" accept="image/*,.heic,.heif" multiple style="display:none"
                   onchange="handleCompPhotos('${compId}', this.files)">
        `;
        grid.appendChild(addBtn);
    }
}

async function handleCompPhotos(compId, files) {
    const comp = getComp(compId);
    const remaining = 6 - comp.photoFilenames.length;

    const processedFiles = await processPhotosBeforeUpload(files, remaining);

    for (const file of processedFiles) {
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