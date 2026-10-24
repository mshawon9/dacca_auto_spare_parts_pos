/* Camera lifecycle shared by the sale modal and the phone scanner. */
window.SaleCamera = class {
    constructor(elementId) {
        this.elementId = elementId;
        this.reader = null;
        this.pending = Promise.resolve();
        this.generation = 0;
    }

    stop() {
        ++this.generation;
        this.pending = this.pending.catch(() => {}).then(async () => {
            if (this.reader) {
                if (this.reader.isScanning) await this.reader.stop();
                this.reader.clear();
                this.reader = null;
            }
        });
        return this.pending;
    }

    async start(onScan) {
        await this.stop();
        const generation = ++this.generation;
        if (!window.isSecureContext || !navigator.mediaDevices) {
            throw new Error('Camera access needs HTTPS (or localhost on this device). Open the app using its HTTPS address.');
        }
        if (!window.Html5Qrcode) throw new Error('Scanner could not load. Refresh the page and try again.');
        const formats = Html5QrcodeSupportedFormats;
        const config = {formatsToSupport: [
            formats.CODE_128, formats.CODE_39, formats.CODE_93, formats.CODABAR,
            formats.EAN_13, formats.EAN_8, formats.UPC_A, formats.UPC_E,
            formats.ITF, formats.RSS_14, formats.RSS_EXPANDED,
            formats.UPC_EAN_EXTENSION
        ]};
        const reader = new Html5Qrcode(this.elementId, config);
        this.reader = reader;
        let decoded = false;
        this.pending = reader.start({facingMode: 'environment'}, {fps: 10}, code => {
            if (decoded || generation !== this.generation) return;
            decoded = true;
            this.stop().then(() => onScan(code));
        }, () => {});
        try {
            await this.pending;
        } catch (_) {
            throw new Error('Could not open camera. Allow camera permission, close other camera apps, and try again.');
        }
    }
};
