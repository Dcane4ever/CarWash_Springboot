document.addEventListener('DOMContentLoaded', function() {
    // Fade in on page load
    document.body.classList.add('page-transition');
    
    // Handle links with fade transition
    document.querySelectorAll('a').forEach(link => {
        link.addEventListener('click', function(e) {
            if (this.href && !this.href.includes('#')) {
                e.preventDefault();
                const href = this.href;
                
                // Fade out
                document.body.classList.add('fade-out');
                
                // Navigate after fade
                setTimeout(() => {
                    window.location.href = href;
                }, 500); // Match this with CSS transition duration
            }
        });
    });
}); 