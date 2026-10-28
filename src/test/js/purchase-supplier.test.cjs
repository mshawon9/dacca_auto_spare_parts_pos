const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const context = {window: {}};
vm.runInNewContext(fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/resources/js/purchase-supplier.js'), 'utf8'), context);
const normalize = context.window.normalizePurchaseSupplierName;
test('supplier punctuation and spacing do not affect matching', () => {
    assert.equal(normalize('GWD LLC'), normalize(' GWD L.L.C '));
    assert.equal(normalize('GWD L.L.C.'), normalize('gwd llc'));
    assert.equal(normalize('ＧＷＤ LLC'), normalize('GWD LLC'));
});
test('different legal names and digits remain distinct', () => {
    assert.notEqual(normalize('GWD Ltd'), normalize('GWD LLC'));
    assert.notEqual(normalize('GWD 1'), normalize('GWD 2'));
    assert.equal(normalize(null), '');
});
