var contextPath = window.contextPath || '';

document.getElementById('washPackageSelect').addEventListener('change', function() {
    const selectedOption = this.options[this.selectedIndex];
    const price = selectedOption.getAttribute('data-price') || '0';
    document.getElementById('selectedPrice').textContent = new Intl.NumberFormat().format(price);
}); 