var wordPageUrl = [[@{/page/word}]];
        window.onload = function() {
            console.log("Navigation script loaded");
            const wordPageBtn = document.getElementById("wordPageBtn");
            wordPageBtn.onclick = function() {
                wordPage();
            };
        };
        function wordPage() {
            window.open(wordPageUrl, "_blank", "toolbar=yes,scrollbars=yes,resizable=yes,top=100,left=500,width=700,height=400");
        }