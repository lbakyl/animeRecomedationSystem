// Search box: type-ahead dropdown (loaded by htmx into #animeSuggestions) and pick-list handling.
document.addEventListener('DOMContentLoaded', function () {
    const input = document.getElementById('animeName');
    const idField = document.getElementById('animeId');
    const box = document.getElementById('animeSuggestions');
    if (!input || !box) {
        return;
    }
    const form = input.form;

    const clear = () => {
        box.innerHTML = '';
    };
    const options = () => Array.from(box.querySelectorAll('button[data-anime-id]'));

    // typing invalidates a previously picked title
    input.addEventListener('input', function () {
        if (idField) {
            idField.value = '';
        }
        if (input.value.trim().length < 2) {
            clear();
        }
    });

    input.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            clear();
        } else if (e.key === 'ArrowDown') {
            const first = options()[0];
            if (first) {
                e.preventDefault();
                first.focus();
            }
        }
    });

    box.addEventListener('keydown', function (e) {
        const items = options();
        const index = items.indexOf(document.activeElement);
        if (e.key === 'ArrowDown' && index < items.length - 1) {
            e.preventDefault();
            items[index + 1].focus();
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            (index > 0 ? items[index - 1] : input).focus();
        } else if (e.key === 'Escape') {
            clear();
            input.focus();
        }
    });

    // picking a title submits the form, so the slider settings are kept
    box.addEventListener('click', function (e) {
        const option = e.target.closest('button[data-anime-id]');
        if (!option) {
            return;
        }
        input.value = option.dataset.name;
        if (idField) {
            idField.value = option.dataset.animeId;
        }
        clear();
        form.requestSubmit();
    });

    document.addEventListener('click', function (e) {
        if (!box.contains(e.target) && e.target !== input) {
            clear();
        }
    });
});
