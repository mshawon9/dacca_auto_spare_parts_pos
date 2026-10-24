(function () {
    const token = location.hash.slice(1);
    history.replaceState(null, '', location.pathname);
    const camera = new SaleCamera('mobileCameraReader');
    const status = text => { document.getElementById('mobileScannerStatus').textContent = text; };
    const start = document.getElementById('startMobileCamera');
    let busy = false;
    let expired = false;
    if (!/^[a-f0-9-]{36}$/.test(token)) {
        status('Invalid connection link. Open Mobile on the sale screen and scan its connection QR.');
        start.disabled = true;
        document.getElementById('sendMobileCode').disabled = true;
        return;
    }
    async function send(code) {
        if (busy || expired) return;
        busy = true;
        try {
            const response = await fetch('/sales/scanner/connections/' + token + '/scans', {
                method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({code: code})
            });
            if (!response.ok) {
                expired = response.status === 410;
                throw new Error(expired ? 'Connection expired. Reconnect from the sale screen.' : 'Could not send. Check the barcode and connection, then try again.');
            }
            status('Barcode sent: ' + code + '. Check the sale screen, add the product, then tap Next Scan.');
            document.getElementById('mobileCode').value = '';
            if (navigator.vibrate) navigator.vibrate(100);
        } catch (error) { status(error.message); }
        finally { busy = false; start.disabled = expired; }
    }
    start.addEventListener('click', async () => {
        if (busy || expired) return;
        start.disabled = true;
        status('Opening camera. Allow permission and hold the product barcode steady.');
        try { await camera.start(send); }
        catch (error) { status(error.message); }
        finally { start.disabled = expired; }
    });
    document.getElementById('stopMobileCamera').addEventListener('click', () => {
        camera.stop(); status('Camera stopped.');
    });
    document.getElementById('mobileCodeForm').addEventListener('submit', async event => {
        event.preventDefault();
        await camera.stop();
        await send(document.getElementById('mobileCode').value.trim());
    });
    window.addEventListener('pagehide', () => camera.stop());
})();
