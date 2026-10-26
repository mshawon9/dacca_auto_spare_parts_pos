(function () {
    'use strict';
    const form = document.getElementById('invoicePhoneForm');
    const input = document.getElementById('invoicePhoto');
    const button = document.getElementById('uploadInvoicePhoto');
    const status = document.getElementById('invoicePhoneStatus');
    const token = window.location.hash.substring(1);
    let selectedFile = null;
    const gallery = document.getElementById('invoiceGallery');
    document.getElementById('chooseInvoiceGallery').addEventListener('click', () => gallery.click());
    [input, gallery].forEach(picker => picker.addEventListener('change', () => {
        selectedFile = picker.files[0] || null;
        status.textContent = selectedFile ? 'Selected: ' + selectedFile.name : 'Choose a photo.';
    }));
    const maxBytes = 3 * 1024 * 1024;
    // Keep the bearer token only in this page's memory, not browser history or requests for assets.
    window.history.replaceState(null, '', window.location.pathname + window.location.search);
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(token)) {
        status.textContent = 'Invalid connection. Scan a new connection from your laptop.';
        button.disabled = true;
        input.disabled = true;
        return;
    }

    function jpeg(canvas, quality) {
        return new Promise(function (resolve, reject) {
            canvas.toBlob(function (blob) {
                if (blob) resolve(blob);
                else reject(new Error('Could not prepare image. Try a different photo.'));
            }, 'image/jpeg', quality);
        });
    }

    async function resize(file) {
        if (file.size > 20 * 1024 * 1024) throw new Error('Choose an image below 20 MB before optimization.');
        if (!['image/jpeg', 'image/png'].includes(file.type)) {
            throw new Error('Choose a JPEG or PNG image.');
        }
        const url = URL.createObjectURL(file);
        try {
            const image = new Image();
            image.src = url;
            await image.decode();
            const scale = Math.min(1, 2000 / Math.max(image.naturalWidth, image.naturalHeight));
            const canvas = document.createElement('canvas');
            canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
            canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
            const context = canvas.getContext('2d');
            if (!context) throw new Error('Image resizing is not supported by this browser.');
            context.fillStyle = '#ffffff';
            context.fillRect(0, 0, canvas.width, canvas.height);
            context.drawImage(image, 0, 0, canvas.width, canvas.height);
            for (const quality of [0.9, 0.8, 0.7, 0.6, 0.5]) {
                const blob = await jpeg(canvas, quality);
                if (blob.size <= maxBytes) return blob;
            }
            throw new Error('Photo is still over 3 MB. Take a simpler or smaller photo.');
        } finally {
            URL.revokeObjectURL(url);
        }
    }

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        const file = selectedFile;
        if (button.disabled) return;
        if (!file) { status.textContent = 'Take a photo or choose an image first.'; return; }
        button.disabled = true;
        input.disabled = true;
        let terminal = false;
        try {
            status.textContent = 'Preparing photo...';
            const blob = await resize(file);
            const data = new FormData();
            data.append('file', blob, 'invoice.jpg');
            status.textContent = 'Uploading photo...';
            const response = await fetch(form.dataset.base + '/connections/' + encodeURIComponent(token) + '/photo', {
                method: 'POST', body: data, credentials: 'omit', cache: 'no-store', referrerPolicy: 'no-referrer'
            });
            if (response.status === 404 || response.status === 410 || response.status === 409) {
                terminal = true;
                throw new Error(response.status === 409
                    ? 'This connection already received a photo. Return to your laptop.'
                    : 'Connection expired or replaced. Scan a new connection from your laptop.');
            }
            if (!response.ok) {
                throw new Error(response.status === 413 ? 'Image is too large for the server.'
                    : 'Upload rejected. Check that the photo is a valid JPEG or PNG and retry.');
            }
            terminal = true;
            input.value = '';
            status.textContent = 'Photo sent. Return to your laptop to review and extract it.';
        } catch (error) {
            status.textContent = error instanceof Error ? error.message : 'Upload failed. Check your connection and retry.';
        } finally {
            button.disabled = terminal;
            input.disabled = terminal;
        }
    });
}());
