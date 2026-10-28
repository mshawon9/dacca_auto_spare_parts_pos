(function () {
    'use strict';
    document.addEventListener('DOMContentLoaded', function () {
        const menu = document.querySelector('.sidebar-menu');
        if (!menu) return;
        const path = window.location.pathname.replace(/\/+$/, '') || '/';
        const links = Array.from(menu.querySelectorAll('a.nav-link[href]')).filter(link => link.getAttribute('href') !== '#');
        const matching = links.filter(link => {
            const target = new URL(link.href, window.location.href).pathname.replace(/\/+$/, '') || '/';
            return path === target || (target !== '/' && path.startsWith(target + '/'));
        }).sort((a, b) => b.getAttribute('href').length - a.getAttribute('href').length);
        const active = matching[0];
        if (active) {
            active.classList.add('active');
            active.setAttribute('aria-current', 'page');
        }
        menu.querySelectorAll('[data-menu-group]').forEach(group => {
            const key = 'dacca.sidebar.' + group.dataset.menuGroup;
            const heading = group.querySelector(':scope > a');
            let stored = null;
            try { stored = window.localStorage.getItem(key); } catch (_) { /* Storage can be disabled. */ }
            const open = group.contains(active) || stored === 'open';
            group.classList.toggle('menu-open', open);
            group.querySelector(':scope > ul').style.display = open ? 'block' : 'none';
            heading.setAttribute('aria-expanded', String(open));
            if (group.contains(active)) heading.classList.add('active');
            function remember(expanded) {
                heading.setAttribute('aria-expanded', String(expanded));
                try { window.localStorage.setItem(key, expanded ? 'open' : 'closed'); } catch (_) { /* Optional preference. */ }
            }
            group.addEventListener('expanded.lte.treeview', () => remember(true));
            group.addEventListener('collapsed.lte.treeview', () => remember(false));
        });
    });
})();
