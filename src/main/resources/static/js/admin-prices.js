var contextPath = (typeof contextPath === "undefined") ? "" : contextPath;

function updatePrice(id) {
  var input = document.getElementById("price-" + id);
  var rawValue = input.value.trim();
  // Remove commas (e.g. "1,500" becomes "1500") and parse as a number.
  var price = parseFloat(rawValue.replace(/,/g, ""));
  if (isNaN(price)) {
    alert("Please enter a valid number (e.g. 1500)");
    return;
  }
  var url = contextPath + "admin/prices/update/" + id + "?price=" + price;
  fetch(url, { method: "POST" })
    .then(response => {
      if (response.ok) {
         alert("Price updated successfully.");
         // Optionally refresh or update the UI
         location.reload();
      } else {
         alert("Error updating price. (Status: " + response.status + ")");
      }
    })
    .catch(err => {
       console.error("Error updating price:", err);
       alert("Error updating price. (See console.)");
    });
}

// Add event listeners to prevent invalid input format errors
document.addEventListener('DOMContentLoaded', function() {
  // Find all price input fields
  var priceInputs = document.querySelectorAll('input[id^="price-"]');
  
  // Add a focus event to handle formatting
  priceInputs.forEach(function(input) {
    // On focus, remove commas for editing
    input.addEventListener('focus', function() {
      var rawValue = this.value.trim();
      this.value = rawValue.replace(/,/g, "");
    });
    
    // On blur, add commas back for display
    input.addEventListener('blur', function() {
      var rawValue = this.value.trim();
      if (!isNaN(parseFloat(rawValue))) {
        var formattedValue = parseFloat(rawValue).toLocaleString();
        this.value = formattedValue;
      }
    });
  });
});