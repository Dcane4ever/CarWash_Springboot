document.addEventListener('DOMContentLoaded', function() {
    // Initialize date range picker
    flatpickr("#reportDateRange", {
        mode: "single",
        dateFormat: "Y-m-d",
        defaultDate: document.getElementById('startDate').value || new Date(),
        onChange: function(selectedDates, dateStr, instance) {
            // Update the hidden input with the selected date
            document.getElementById('startDate').value = dateStr;
        }
    });

    // Initialize charts if data exists
    initializeCharts();

    // CSV Download Button Handler
    const downloadCsvButton = document.getElementById('downloadCsvButton');
    if (downloadCsvButton) {
        downloadCsvButton.addEventListener('click', function() {
            const selectedDate = document.getElementById('startDate').value;
            const selectedReportType = document.querySelector('select[name="reportType"]').value;
            
            // Construct the URL for CSV export
            let csvUrl = contextPath + 'admin/reports/export/csv?reportType=' + selectedReportType;
            if (selectedDate) {
                csvUrl += `&startDateParam=${selectedDate}`;
            }
            
            // Trigger the download
            window.location.href = csvUrl;
        });
    }
});

function initializeCharts() {
    // Revenue Trend Chart (New)
    if (typeof revenueTrendData !== 'undefined' && Object.keys(revenueTrendData).length > 0) {
        const revenueTrendCtx = document.getElementById('revenueTrendChart').getContext('2d');
        new Chart(revenueTrendCtx, {
            type: 'line',
            data: {
                labels: Object.keys(revenueTrendData),
                datasets: [{
                    label: 'Revenue Trend',
                    data: Object.values(revenueTrendData),
                    borderColor: 'rgb(75, 192, 192)',
                    tension: 0.1,
                    fill: false
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return '₱' + value.toLocaleString();
                            }
                        }
                    }
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return '₱' + context.raw.toLocaleString();
                            }
                        }
                    }
                }
            }
        });
    }

    // Service Distribution Chart
    if (typeof serviceDistribution !== 'undefined' && Object.keys(serviceDistribution).length > 0) {
        const serviceCtx = document.getElementById('serviceChart').getContext('2d');
        new Chart(serviceCtx, {
            type: 'pie',
            data: {
                labels: Object.keys(serviceDistribution),
                datasets: [{
                    data: Object.values(serviceDistribution),
                    backgroundColor: ['#2ecc71', '#e74c3c', '#f1c40f']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const value = context.raw;
                                const percent = ((value / total) * 100).toFixed(1);
                                return `${context.label}: ${value} (${percent}%)`;
                            }
                        }
                    },
                    legend: {
                        labels: {
                            generateLabels: function(chart) {
                                const data = chart.data;
                                if (data.labels.length && data.datasets.length) {
                                    const total = data.datasets[0].data.reduce((a, b) => a + b, 0);
                                    return data.labels.map((label, i) => {
                                        const value = data.datasets[0].data[i];
                                        const percent = ((value / total) * 100).toFixed(1);
                                        return {
                                            text: `${label} (${percent}%)`,
                                            fillStyle: data.datasets[0].backgroundColor[i],
                                            hidden: isNaN(data.datasets[0].data[i]) || chart.getDataVisibility(i) === false,
                                            index: i
                                        };
                                    });
                                }
                                return [];
                            }
                        }
                    }
                }
            }
        });
    }
} 