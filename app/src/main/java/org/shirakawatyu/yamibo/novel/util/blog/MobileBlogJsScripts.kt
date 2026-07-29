package org.shirakawatyu.yamibo.novel.util.blog

internal object MobileBlogJsScripts {
    val ENHANCEMENTS_JS = """
        (function() {
            var body = document.body;
            if (!body || body.id !== 'home' || !body.classList.contains('pg_space')) return;

            var pageUrl;
            try { pageUrl = new URL(location.href); } catch (e) { return; }
            if (
                String(pageUrl.searchParams.get('mod') || '').toLowerCase() !== 'space' ||
                String(pageUrl.searchParams.get('do') || '').toLowerCase() !== 'blog' ||
                !/^[1-9]\d*$/.test(pageUrl.searchParams.get('id') || '')
            ) return;

            function installInviteNavigation() {
                if (window.__yamiboMobileBlogInviteV1) return;
                window.__yamiboMobileBlogInviteV1 = true;
                var inviteLink = document.getElementById('a_invite');
                if (inviteLink) {
                    inviteLink.classList.remove('dialog');
                    inviteLink.removeAttribute('onclick');
                }
                document.addEventListener('click', function(event) {
                    var link = event.target && event.target.closest
                        ? event.target.closest('#a_invite[href]')
                        : null;
                    if (!link) return;
                    event.preventDefault();
                    event.stopPropagation();
                    event.stopImmediatePropagation();
                    try {
                        var inviteUrl = new URL(link.href, location.href);
                        inviteUrl.searchParams.set('mobile', '2');
                        location.assign(inviteUrl.href);
                    } catch (e) {
                        location.assign(link.href);
                    }
                }, true);
            }
            installInviteNavigation();

            if (window.__yamiboMobileBlogEnhancementsV1) return;
            window.__yamiboMobileBlogEnhancementsV1 = true;
            if (!window.AndroidBlogReaction) return;

            var foot = document.querySelector('.viewthread .plc > .threadlist_foot') ||
                document.querySelector('.threadlist_foot');
            if (!foot || document.getElementById('yamibo-blog-reactions')) return;

            var ownerUid = pageUrl.searchParams.get('uid') || '';
            if (!/^[1-9]\d*$/.test(ownerUid)) {
                var author = document.querySelector(
                    '.viewthread .authi a[href*="mod=space"][href*="uid="]'
                );
                if (author) {
                    try { ownerUid = new URL(author.href, location.href).searchParams.get('uid') || ''; }
                    catch (e) {}
                }
            }
            var blogId = pageUrl.searchParams.get('id') || '';
            if (!/^[1-9]\d*$/.test(ownerUid)) return;

            if (!document.getElementById('yamibo-mobile-blog-reaction-style')) {
                var style = document.createElement('style');
                style.id = 'yamibo-mobile-blog-reaction-style';
                style.textContent = [
                    '#yamibo-blog-reactions{margin:16px 0 10px;padding:4px 0 12px;',
                    'background:transparent;color:var(--dz-FC-333,#333)}',
                    '#yamibo-blog-reactions .ybr-options{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:8px}',
                    '#yamibo-blog-reactions .ybr-option{appearance:none;border:0;background:transparent;',
                    'color:var(--dz-FC-color,#c06f3e);min-width:0;padding:0;text-align:center}',
                    '#yamibo-blog-reactions .ybr-option:disabled{opacity:.55}',
                    '#yamibo-blog-reactions .ybr-meter{height:64px;display:flex;align-items:flex-end;',
                    'justify-content:center;margin-bottom:6px}',
                    '#yamibo-blog-reactions .ybr-bar{position:relative;width:24px;min-height:2px;border-radius:2px 2px 0 0;',
                    'background:#d43800;transition:height .2s ease}',
                    '#yamibo-blog-reactions .ybr-bar.ac2{background:#5e9f25}',
                    '#yamibo-blog-reactions .ybr-bar.ac3{background:#f08a24}',
                    '#yamibo-blog-reactions .ybr-bar.ac4{background:#448aca}',
                    '#yamibo-blog-reactions .ybr-count{position:absolute;left:50%;bottom:100%;',
                    'transform:translate(-50%,-3px);font-size:11px;line-height:1;color:var(--dz-FC-666,#666)}',
                    '#yamibo-blog-reactions .ybr-icon{display:block;width:34px;height:34px;',
                    'object-fit:contain;margin:0 auto 4px}',
                    '#yamibo-blog-reactions .ybr-label{display:block;font-size:12px;white-space:nowrap;',
                    'overflow:hidden;text-overflow:ellipsis}',
                    '#yamibo-blog-reactions .ybr-friends-title{font-size:13px;font-weight:600;',
                    'margin:16px 0 9px}',
                    '#yamibo-blog-reactions .ybr-users{display:flex;gap:10px;overflow-x:auto;',
                    'padding:0 0 4px;scrollbar-width:none}',
                    '#yamibo-blog-reactions .ybr-users::-webkit-scrollbar{display:none}',
                    '#yamibo-blog-reactions .ybr-user{flex:0 0 52px;min-width:0;text-align:center;',
                    'color:var(--dz-FC-color,#c06f3e)}',
                    '#yamibo-blog-reactions .ybr-avatar{display:block;width:44px;height:44px;',
                    'object-fit:cover;border-radius:6px;margin:0 auto 4px;background:var(--dz-BG-5,#f4f4f4)}',
                    '#yamibo-blog-reactions .ybr-name{display:block;font-size:11px;white-space:nowrap;',
                    'overflow:hidden;text-overflow:ellipsis}',
                    '#yamibo-blog-reactions .ybr-status{font-size:12px;color:var(--dz-FC-999,#999);',
                    'text-align:center;padding:12px 0}',
                    '#yamibo-blog-reactions .ybr-status:empty{display:none}'
                ].join('');
                (document.head || document.documentElement).appendChild(style);
            }

            var section = document.createElement('section');
            section.id = 'yamibo-blog-reactions';
            section.innerHTML =
                '<div class="ybr-options"></div>' +
                '<div class="ybr-friends-title"></div>' +
                '<div class="ybr-users"></div>' +
                '<div class="ybr-status">正在加载表态…</div>';
            foot.parentNode.insertBefore(section, foot);

            var optionsBox = section.querySelector('.ybr-options');
            var friendsTitle = section.querySelector('.ybr-friends-title');
            var usersBox = section.querySelector('.ybr-users');
            var statusBox = section.querySelector('.ybr-status');
            var activeRequest = '';
            var busy = false;

            function makeRequestId() {
                return String(Date.now()) + '-' + Math.random().toString(36).slice(2);
            }

            function setBusy(nextBusy) {
                busy = nextBusy;
                section.querySelectorAll('.ybr-option').forEach(function(button) {
                    button.disabled = nextBusy;
                });
            }

            function render(payload) {
                var options = Array.isArray(payload.options) ? payload.options : [];
                var users = Array.isArray(payload.users) ? payload.users : [];
                var maximum = Math.max.apply(null, options.map(function(option) {
                    return Number(option.count) || 0;
                }).concat([1]));
                optionsBox.textContent = '';
                options.forEach(function(option) {
                    var count = Number(option.count) || 0;
                    var button = document.createElement('button');
                    button.type = 'button';
                    button.className = 'ybr-option';
                    button.setAttribute('data-clickid', String(option.clickId || ''));

                    var meter = document.createElement('span');
                    meter.className = 'ybr-meter';
                    var bar = document.createElement('span');
                    bar.className = 'ybr-bar ' + String(option.barClass || '');
                    bar.style.height = (count > 0 ? Math.max(8, Math.round(count / maximum * 64)) : 2) + 'px';
                    var countNode = document.createElement('span');
                    countNode.className = 'ybr-count';
                    countNode.textContent = String(count);
                    bar.appendChild(countNode);
                    meter.appendChild(bar);

                    var icon = document.createElement('img');
                    icon.className = 'ybr-icon';
                    icon.alt = '';
                    icon.src = String(option.iconUrl || '');
                    var label = document.createElement('span');
                    label.className = 'ybr-label';
                    label.textContent = String(option.label || '');
                    button.appendChild(meter);
                    button.appendChild(icon);
                    button.appendChild(label);
                    optionsBox.appendChild(button);
                });

                var total = Number(payload.totalCount) || 0;
                friendsTitle.textContent = total > 0 ? '刚表态过的朋友（' + total + ' 人）' : '';
                usersBox.textContent = '';
                users.forEach(function(user) {
                    var link = document.createElement('a');
                    link.className = 'ybr-user';
                    link.href = 'home.php?mod=space&uid=' +
                        encodeURIComponent(String(user.uid || '')) + '&do=profile&mobile=2';
                    var avatar = document.createElement('img');
                    avatar.className = 'ybr-avatar';
                    avatar.alt = '';
                    avatar.src = String(user.avatarUrl || '');
                    avatar.title = String(user.reaction || '');
                    var name = document.createElement('span');
                    name.className = 'ybr-name';
                    name.textContent = String(user.username || '');
                    link.appendChild(avatar);
                    link.appendChild(name);
                    usersBox.appendChild(link);
                });
                statusBox.textContent = String(payload.message || '');
                setBusy(false);
            }

            window.__yamiboBlogReactionReceive = function(requestId, payload) {
                if (String(requestId) !== activeRequest) return;
                if (payload && payload.error) {
                    statusBox.textContent = String(payload.error);
                    setBusy(false);
                    return;
                }
                render(payload || {});
            };

            optionsBox.addEventListener('click', function(event) {
                var button = event.target && event.target.closest
                    ? event.target.closest('.ybr-option[data-clickid]')
                    : null;
                if (!button || busy) return;
                var clickId = button.getAttribute('data-clickid') || '';
                if (!/^[1-9]\d*$/.test(clickId)) return;
                setBusy(true);
                statusBox.textContent = '正在提交表态…';
                activeRequest = makeRequestId();
                window.AndroidBlogReaction.react(ownerUid, blogId, clickId, activeRequest);
            });

            activeRequest = makeRequestId();
            window.AndroidBlogReaction.load(ownerUid, blogId, activeRequest);
        })();
    """.trimIndent()
}
