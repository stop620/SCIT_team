document.addEventListener('DOMContentLoaded', function() {
    const draggable = document.querySelectorAll('.wordPoolItem');
    const droppable = document.querySelectorAll('.quizBlank');
    const wordPool = document.querySelector('.quizBlankWordPool');
    const resetBtn = document.getElementById('resetDragDropBtn');

    // 원래 wordPoolItem 순서 저장
    const originalItems = Array.from(draggable).map(item => item.cloneNode(true));

    draggable.forEach(item => {
        item.addEventListener('dragstart', dragStart);
        item.addEventListener('dragend', dragEnd);
    });

    droppable.forEach(item => {
        item.addEventListener('dragover', dragOver);
        item.addEventListener('dragleave', dragLeave);
        item.addEventListener('drop', dragDrop);
    });

    function dragStart(ev) {
        ev.dataTransfer.effectAllowed = "move";
        ev.dataTransfer.setData("text/plain", ev.target.textContent);
        ev.target.classList.add('dragging');
    }

    function dragEnd(ev) {
        ev.target.classList.remove('dragging');
    }

    function dragOver(ev) {
        ev.preventDefault();
        ev.currentTarget.classList.add('drag-over');
    }

    function dragLeave(ev) {
        ev.currentTarget.classList.remove('drag-over');
    }

    function dragDrop(ev) {
        ev.preventDefault();
        const draggingItem = document.querySelector('.dragging');
        ev.currentTarget.classList.remove('drag-over');
        if (draggingItem && ev.currentTarget.children.length === 0) {
            ev.currentTarget.appendChild(draggingItem);
        }
    }

    // 초기화 버튼 이벤트
    if (resetBtn && wordPool) {
        resetBtn.addEventListener('click', function() {
            // quizBlank에 들어간 아이템들 모두 wordPool로 이동
            droppable.forEach(blank => {
                while (blank.firstChild) {
                    wordPool.appendChild(blank.firstChild);
                }
            });
            // wordPool을 원래 순서로 복구
            wordPool.innerHTML = '';
            originalItems.forEach(item => wordPool.appendChild(item.cloneNode(true)));
            // 이벤트 다시 연결
            const newDraggable = wordPool.querySelectorAll('.wordPoolItem');
            newDraggable.forEach(item => {
                item.addEventListener('dragstart', dragStart);
                item.addEventListener('dragend', dragEnd);
            });
        });
    }
});