const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const context = {window: {}};
vm.runInNewContext(fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/resources/js/purchase-date.js'), 'utf8'), context);
const dates = context.window.PurchaseDate;

test('formats AI and server ISO dates for display', () => {
    assert.equal(dates.fromIso('2026-10-26'), '26-10-2026');
    assert.equal(dates.fromIso('2026-01-03'), '03-01-2026');
});

test('submits a day-first date as ISO without changing the day', () => {
    assert.equal(dates.toIso('26-10-2026'), '2026-10-26');
    assert.equal(dates.toIso('03-01-2026'), '2026-01-03');
    assert.equal(dates.fromIso(dates.toIso('31-12-2026')), '31-12-2026');
});

test('validates leap days using the full Gregorian calendar', () => {
    assert.equal(dates.toIso('29-02-2024'), '2024-02-29');
    assert.equal(dates.toIso('29-02-2000'), '2000-02-29');
    assert.equal(dates.toIso('29-02-2026'), '');
    assert.equal(dates.toIso('29-02-1900'), '');
});

test('rejects invalid dates and unsupported formats', () => {
    for (const input of ['', null, '31-04-2026', '00-10-2026', '01-00-2026', '01-13-2026', '1-1-2026', '2026-10-26', '26/10/2026']) {
        assert.equal(dates.toIso(input), '', String(input));
    }
    assert.equal(dates.fromIso('2026-02-30'), '');
    assert.equal(dates.fromIso('2026-13-01'), '');
});
