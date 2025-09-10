document.addEventListener("DOMContentLoaded", function () {

    document.querySelectorAll('.delete-btn').forEach(button => {
        button.addEventListener('click', (event) => {
            console.log('삭제클릭');

            if(confirm('삭제하시겠습니까?')) {
                const parentDiv = event.target.closest('div');
                console.log(parentDiv);
                if (parentDiv) {
                    const bookId = parentDiv.getAttribute('data-bookId');
                    const wordId = parentDiv.getAttribute('data-wordId');

                    console.log('삭제할 단어장: ' + bookId + " 삭제할 단어: " + wordId);
                    removeWord(bookId, wordId);
                }
            }
        });
    });
});

function removeWord(bookId, wordId) {

    fetch(`/mochilearn/api/wordbook/delete?bookId=${bookId}&wordId=${wordId}`,
        {
            method: 'DELETE',
            headers: {'Content-Type': 'application/json'}
        })
        .then(response => {
            if (response.ok) {
                return response.json();
            }
        })
        .then(data => {
            console.log('단어 삭제 성공', data);
            window.location.reload();
        })
        .catch(error => {
            console.error('Error', error);
            alert('단어 삭제에 실패했습니다.');
        });
}