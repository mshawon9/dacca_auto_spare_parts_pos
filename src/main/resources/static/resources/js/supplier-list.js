$(function () {
    if ($('#supplierPageData').data('open-create-modal') === true) {
        bootstrap.Modal.getOrCreateInstance(document.getElementById('supplierModal')).show();
    }

    function valueOrDash(value) {
        return value ? value : '-';
    }

    $('#btnAddSupplier').on('click', function () {
        $('#supplierForm').attr('action', '/suppliers');
        $('#supplierModalTitle').text('Add Supplier');
        $('#supplierSubmitButton').text('Add Supplier');
        $('#name, #contactPerson, #trnNumber, #phone, #email').val('');
        $('#address').val('');
        $('#active').prop('checked', true);
    });

    $('.btn-supplier-edit').on('click', function () {
        const $btn = $(this);
        $('#supplierForm').attr('action', '/suppliers/' + $btn.data('id') + '/edit');
        $('#supplierModalTitle').text('Edit Supplier');
        $('#supplierSubmitButton').text('Update Supplier');
        $('#name').val($btn.data('name') || '');
        $('#contactPerson').val($btn.data('contact-person') || '');
        $('#trnNumber').val($btn.data('trn-number') || '');
        $('#phone').val($btn.data('phone') || '');
        $('#email').val($btn.data('email') || '');
        $('#address').val($btn.data('address') || '');
        $('#active').prop('checked', String($btn.data('active')) === 'true');
    });

    $('.btn-supplier-details').on('click', function () {
        const $btn = $(this);
        $('#supplierDetailName').text(valueOrDash($btn.data('name')));
        $('#supplierDetailTrn').text(valueOrDash($btn.data('trn-number')));
        $('#supplierDetailContact').text(valueOrDash($btn.data('contact-person')));
        $('#supplierDetailPhone').text(valueOrDash($btn.data('phone')));
        $('#supplierDetailEmail').text(valueOrDash($btn.data('email')));
        $('#supplierDetailAddress').text(valueOrDash($btn.data('address')));
        $('#supplierDetailStatus').text(String($btn.data('active')) === 'true' ? 'Active' : 'Inactive');
    });
});
