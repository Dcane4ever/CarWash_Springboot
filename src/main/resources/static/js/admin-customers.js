var contextPath = window.contextPath || '';
 
$.ajax({
    url: contextPath + `/admin/customers/delete/${id}`,
    // ... existing code ...
}); 