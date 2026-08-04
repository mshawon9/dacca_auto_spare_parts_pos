$(function () {
    if ($('#customerPageData').data('open-create-modal') === true) {
        bootstrap.Modal.getOrCreateInstance(document.getElementById('customerModal')).show();
    }

    function valueOrDash(value) {
        return value ? value : '-';
    }

    $('#btnAddCustomer').on('click', function () {
        $('#customerForm').attr('action', '/customers');
        $('#customerModalTitle').text('Add Customer');
        $('#customerSubmitButton').text('Add Customer');
        $('#name, #contactPerson, #trnNumber, #phone, #email').val('');
        $('#address').val('');
        $('#defaultCreditDays').val('');
        $('#active').prop('checked', true);
        $('#alwaysCredit').prop('checked', false);
    });

    $('.btn-customer-edit').on('click', function () {
        const $btn = $(this);
        $('#customerForm').attr('action', '/customers/' + $btn.data('id') + '/edit');
        $('#customerModalTitle').text('Edit Customer');
        $('#customerSubmitButton').text('Update Customer');
        $('#name').val($btn.data('name') || '');
        $('#contactPerson').val($btn.data('contact-person') || '');
        $('#trnNumber').val($btn.data('trn-number') || '');
        $('#phone').val($btn.data('phone') || '');
        $('#email').val($btn.data('email') || '');
        $('#address').val($btn.data('address') || '');
        $('#defaultCreditDays').val($btn.data('default-credit-days') || '');
        $('#active').prop('checked', String($btn.data('active')) === 'true');
        $('#alwaysCredit').prop('checked', String($btn.data('always-credit')) === 'true');
    });

    $('.btn-customer-details').on('click', function () {
        const $btn = $(this);
        $('#customerDetailName').text(valueOrDash($btn.data('name')));
        $('#customerDetailTrn').text(valueOrDash($btn.data('trn-number')));
        $('#customerDetailContact').text(valueOrDash($btn.data('contact-person')));
        $('#customerDetailPhone').text(valueOrDash($btn.data('phone')));
        $('#customerDetailEmail').text(valueOrDash($btn.data('email')));
        $('#customerDetailAddress').text(valueOrDash($btn.data('address')));
        $('#customerDetailCredit').text(String($btn.data('always-credit')) === 'true' ? 'Always Credit' : 'Normal');
        $('#customerDetailCreditDays').text($btn.data('default-credit-days') ? $btn.data('default-credit-days') + ' days' : '-');
        $('#customerDetailStatus').text(String($btn.data('active')) === 'true' ? 'Active' : 'Inactive');
    });
});
