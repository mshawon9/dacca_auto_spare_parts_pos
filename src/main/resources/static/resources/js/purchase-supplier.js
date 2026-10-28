(function (window) {
    'use strict';
    window.normalizePurchaseSupplierName = function (name) {
        return String(name || '').normalize('NFKC').toLowerCase().replace(/[^\p{L}\p{N}]/gu, '');
    };
})(window);
