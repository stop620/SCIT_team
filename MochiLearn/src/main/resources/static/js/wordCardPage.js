document.addEventListener("DOMContentLoaded", function () {

    document.querySelectorAll('.delete-word-btn').forEach(button => {
        button.addEventListener('click', (event) => {
            console.log('단어 삭제 클릭');

            if(confirm('단어를 삭제하시겠습니까?')) {
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

    document.querySelector('.delete-book-btn').addEventListener('click', (event) => {
        console.log('단어장 삭제 클릭');

        if(confirm('단어장을 삭제하시겠습니까?')) {
            const parentDiv = event.target.closest('div');
            console.log(parentDiv);
            if(parentDiv) {
                const bookId = parentDiv.getAttribute('data-bookId');

                console.log('삭제할 단어장: ' + bookId);
                removeBook(bookId);
            }
        }
    })

});

function removeWord(bookId, wordId) {

    fetch(`/mochilearn/api/word/delete?bookId=${bookId}&wordId=${wordId}`,
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

function removeBook(bookId) {

    fetch(`/mochilearn/api/wordbook/delete?bookId=${bookId}`,
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
            console.log('단어장 삭제 성공', data);
            window.location.href = "../page/word";
        })
        .catch(error => {
            console.error('Error', error);
            alert('단어장 삭제에 실패했습니다.');
        });
}