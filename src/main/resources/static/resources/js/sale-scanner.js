$(function () {
    const element = document.getElementById('saleScannerModal');
    const modal = bootstrap.Modal.getOrCreateInstance(element);
    const camera = new SaleCamera('saleCameraReader');
    let connection = null;
    let acknowledged = 0;
    let timer = null;
    let receiving = false;
    let changingMode = false;
    let lastError = null;
    const status = text => $('#scannerStatus').text(text);

    async function request(url, options) {
        const response = await fetch(url, Object.assign({cache: 'no-store'}, options));
        if (!response.ok) {
            const error = new Error(response.status === 410
                ? 'Phone connection expired. Choose Mobile to connect again.' : 'Scanner request failed. Please try again.');
            error.status = response.status;
            throw error;
        }
        const body = await response.text();
        return body ? JSON.parse(body) : null;
    }

    async function selectCode(code) {
        const query = new URLSearchParams({code: code, storeId: $('#storeId').val()});
        if ($('#customerId').val()) query.set('customerId', $('#customerId').val());
        const response = await fetch('/sales/scanner/product?' + query, {cache: 'no-store'});
        if (!response.ok) throw new Error(response.status === 404
            ? 'No active product matches this barcode. Check the product barcode or search manually.'
            : 'Could not load the scanned product. Check the warehouse and try again.');
        const product = await response.json();
        $(document).trigger('sale:scanned-product', [product]);
        if (element.classList.contains('show')) {
            $(element).one('hidden.bs.modal', () => $('#quickQuantity').trigger('focus').select());
            modal.hide();
        }
    }

    async function disconnect() {
        clearTimeout(timer);
        const old = connection;
        connection = null;
        $('#openSaleScanner').removeClass('btn-success').addClass('btn-outline-primary');
        if (old) await request('/sales/scanner/connections/' + old.token, {method: 'DELETE'}).catch(() => {});
    }

    async function poll() {
        const active = connection;
        if (!active) return;
        try {
            if (Date.now() >= Date.parse(active.expiresAt)) throw new Error('Phone connection expired. Choose Mobile to connect again.');
            // Leave the current selection intact until the cashier adds or clears it.
            if (!receiving && !$('#productSelect').val()) {
                receiving = true;
                const scan = await request('/sales/scanner/connections/' + active.token + '?acknowledged=' + acknowledged);
                if (connection !== active) return;
                lastError = null;
                if (scan && scan.sequence > acknowledged) {
                    acknowledged = scan.sequence;
                    await selectCode(scan.code);
                }
            }
        } catch (error) {
            status(error.message);
            if (lastError !== error.message) $(document).trigger('sale:scanner-error', [error.message]);
            lastError = error.message;
            if (error.status === 410 || Date.now() >= Date.parse(active.expiresAt)) await disconnect();
        } finally {
            receiving = false;
            if (connection === active) timer = setTimeout(poll, lastError ? 5000 : 1200);
        }
    }

    $('#openSaleScanner').on('click', function () {
        if (!$('#storeId').val()) {
            $(document).trigger('sale:scanner-error', ['Select a warehouse before scanning.']);
            return;
        }
        modal.show();
    });

    $('[data-scan-mode]').on('click', async function () {
        if (changingMode) return;
        changingMode = true;
        $('[data-scan-mode]').prop('disabled', true).removeClass('active');
        $(this).addClass('active');
        const mode = $(this).data('scan-mode');
        try {
            await camera.stop();
            $('#phonePairing').toggleClass('d-none', mode !== 'mobile');
            if (mode === 'mobile') {
                if (!connection) {
                    connection = await request('/sales/scanner/connections', {method: 'POST'});
                    acknowledged = 0;
                    poll();
                }
                const link = location.origin + '/sales/scanner/mobile#' + connection.token;
                $('#phoneScannerLink').val(link);
                $('#phonePairingQr').empty();
                new QRCode(document.getElementById('phonePairingQr'), {text: link, width: 200, height: 200});
                $('#openSaleScanner').removeClass('btn-outline-primary').addClass('btn-success');
                status(location.hostname === 'localhost' || location.hostname === '127.0.0.1'
                    ? 'This localhost link cannot be opened on a phone. Reopen the sale page using an HTTPS address reachable by both devices.'
                    : 'Waiting for phone scans. You may close this modal and continue the sale.');
            } else {
                await disconnect();
                status('Allow camera access, then hold the code steady in view.');
                await camera.start(async code => {
                    try { await selectCode(code); }
                    catch (error) { status(error.message + ' Choose Camera to retry.'); }
                });
            }
        } catch (error) { status(error.message); }
        finally {
            changingMode = false;
            $('[data-scan-mode]').prop('disabled', false);
            if (!element.classList.contains('show')) camera.stop();
        }
    });
    $(element).on('hide.bs.modal', () => camera.stop());
    $('#disconnectPhoneScanner').on('click', async () => {
        await disconnect();
        $('#phonePairing').addClass('d-none');
        status('Phone disconnected. Choose an option to scan again.');
    });
    $('#copyScannerLink').on('click', async () => {
        try {
            await navigator.clipboard.writeText($('#phoneScannerLink').val());
            status('Link copied. Open it on your phone.');
        } catch (_) { $('#phoneScannerLink').trigger('focus').select(); status('Select and copy the link above.'); }
    });
    window.addEventListener('pagehide', () => {
        camera.stop();
        if (connection) fetch('/sales/scanner/connections/' + connection.token, {method: 'DELETE', keepalive: true});
    });
});
