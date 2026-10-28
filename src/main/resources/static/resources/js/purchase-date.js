(function (root) {
    'use strict';

    function toIso(value) {
        const match = /^(\d{2})-(\d{2})-(\d{4})$/.exec(String(value || '').trim());
        if (!match) return '';
        const day = Number(match[1]);
        const month = Number(match[2]);
        const year = Number(match[3]);
        if (year < 1000) return '';
        const date = new Date(Date.UTC(year, month - 1, day));
        if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return '';
        return match[3] + '-' + match[2] + '-' + match[1];
    }

    function fromIso(value) {
        const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value || '').trim());
        if (!match) return '';
        const display = match[3] + '-' + match[2] + '-' + match[1];
        return toIso(display) ? display : '';
    }

    root.PurchaseDate = {toIso: toIso, fromIso: fromIso};
})(window);
