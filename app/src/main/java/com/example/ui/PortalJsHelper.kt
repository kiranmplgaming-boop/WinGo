package com.example.ui

object PortalJsHelper {

    fun generateAutoLoginScript(phone: String, pass: String): String {
        val safePhone = phone.replace("'", "\\'").replace("\"", "\\\"")
        val safePass = pass.replace("'", "\\'").replace("\"", "\\\"")

        return """
            (function() {
                try {
                    const TARGET_PHONE = '$safePhone';
                    const TARGET_PASS = '$safePass';

                    function notify(msg) {
                        if (window.AndroidBridge && window.AndroidBridge.onStatusUpdate) {
                            window.AndroidBridge.onStatusUpdate(msg);
                        }
                    }

                    function setNativeValue(element, value) {
                        if (!element) return;
                        element.focus();
                        const valueSetter = Object.getOwnPropertyDescriptor(element, 'value')?.set;
                        const prototype = Object.getPrototypeOf(element);
                        const prototypeValueSetter = Object.getOwnPropertyDescriptor(prototype, 'value')?.set;
                        if (prototypeValueSetter && valueSetter !== prototypeValueSetter) {
                            prototypeValueSetter.call(element, value);
                        } else if (valueSetter) {
                            valueSetter.call(element, value);
                        } else {
                            element.value = value;
                        }
                        element.dispatchEvent(new Event('input', { bubbles: true }));
                        element.dispatchEvent(new Event('change', { bubbles: true }));
                        element.dispatchEvent(new Event('blur', { bubbles: true }));
                    }

                    function findInputs() {
                        const inputs = Array.from(document.querySelectorAll('input:not([type="hidden"]):not([type="checkbox"]):not([type="radio"])'));
                        let passInput = inputs.find(i => 
                            i.type === 'password' || 
                            (i.placeholder && /password|pass|密码/i.test(i.placeholder)) ||
                            (i.name && /password|pass/i.test(i.name))
                        );

                        let phoneInput = inputs.find(i => 
                            i !== passInput && (
                                i.type === 'tel' || 
                                i.type === 'number' ||
                                (i.placeholder && /phone|mobile|number|account|user|手机|电话|账号/i.test(i.placeholder)) ||
                                (i.name && /phone|mobile|account|user/i.test(i.name))
                            )
                        );

                        if (!phoneInput && passInput) {
                            const passIndex = inputs.indexOf(passInput);
                            for (let i = passIndex - 1; i >= 0; i--) {
                                const inp = inputs[i];
                                if (inp.type === 'text' || inp.type === 'tel' || inp.type === 'number' || !inp.type) {
                                    phoneInput = inp;
                                    break;
                                }
                            }
                        }

                        if (!phoneInput && inputs.length >= 2 && inputs[0] !== passInput) {
                            phoneInput = inputs[0];
                        }

                        return { phoneInput, passInput };
                    }

                    function checkAgreements() {
                        const checkboxes = document.querySelectorAll('input[type="checkbox"], .van-checkbox, [class*="agree" i], [class*="checkbox" i]');
                        for (const cb of checkboxes) {
                            if (cb.tagName === 'INPUT' && !cb.checked) {
                                cb.checked = true;
                                cb.dispatchEvent(new Event('change', { bubbles: true }));
                            } else if (cb.classList.contains('van-checkbox') && !cb.classList.contains('van-checkbox--checked')) {
                                cb.click();
                            }
                        }
                    }

                    function findLoginButton() {
                        const candidates = Array.from(document.querySelectorAll('button, input[type="submit"], div[role="button"], .van-button, .btn, [class*="login" i], [class*="submit" i], a'));
                        for (const el of candidates) {
                            const text = (el.innerText || el.textContent || el.value || '').trim();
                            if (/^(login|log in|sign in|登录|立即登录|登入)$/i.test(text) || 
                                (text.toLowerCase().includes('login') && !text.toLowerCase().includes('register')) ||
                                (text.includes('登录') && !text.includes('注册'))) {
                                return el;
                            }
                        }

                        const pass = document.querySelector('input[type="password"]');
                        if (pass) {
                            const form = pass.closest('form') || pass.closest('.van-form') || pass.parentElement?.parentElement;
                            if (form) {
                                const btn = form.querySelector('button, [role="button"], .van-button');
                                if (btn) return btn;
                            }
                        }
                        return null;
                    }

                    let attempts = 0;
                    const maxAttempts = 20;

                    const interval = setInterval(() => {
                        attempts++;
                        const { phoneInput, passInput } = findInputs();

                        if (phoneInput && passInput) {
                            clearInterval(interval);
                            notify('Credentials fields located. Auto-filling...');
                            
                            checkAgreements();
                            setNativeValue(phoneInput, TARGET_PHONE);
                            setNativeValue(passInput, TARGET_PASS);

                            if (window.AndroidBridge && window.AndroidBridge.onAutoFillSuccess) {
                                window.AndroidBridge.onAutoFillSuccess();
                            }

                            setTimeout(() => {
                                checkAgreements();
                                const btn = findLoginButton();
                                if (btn) {
                                    notify('Clicking Login button...');
                                    btn.click();
                                    btn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
                                    if (window.AndroidBridge && window.AndroidBridge.onLoginButtonClicked) {
                                        window.AndroidBridge.onLoginButtonClicked();
                                    }

                                    // Watch for URL or Hash change after login
                                    let pollCount = 0;
                                    const watcher = setInterval(() => {
                                        pollCount++;
                                        const hash = window.location.hash || '';
                                        const href = window.location.href || '';
                                        if (hash && !hash.includes('login')) {
                                            clearInterval(watcher);
                                            if (window.AndroidBridge && window.AndroidBridge.onLoginRedirect) {
                                                window.AndroidBridge.onLoginRedirect(href);
                                            }
                                        } else if (pollCount >= 10) {
                                            clearInterval(watcher);
                                            if (window.location.hash.includes('login')) {
                                                window.location.hash = '#/';
                                            }
                                            if (window.AndroidBridge && window.AndroidBridge.onLoginRedirect) {
                                                window.AndroidBridge.onLoginRedirect(window.location.href);
                                            }
                                        }
                                    }, 500);

                                } else {
                                    notify('Fields filled! Please tap login button on screen.');
                                }
                            }, 500);

                        } else if (attempts >= maxAttempts) {
                            clearInterval(interval);
                            notify('Ready. Use "Auto Login" to retry filling.');
                        }
                    }, 400);

                } catch (e) {
                    if (window.AndroidBridge && window.AndroidBridge.onStatusUpdate) {
                        window.AndroidBridge.onStatusUpdate('JS note: ' + e.message);
                    }
                }
            })();
        """.trimIndent()
    }

    fun generateGoToHomeScript(): String {
        return """
            (function() {
                try {
                    if (window.location.hash.includes('login')) {
                        window.location.hash = '#/';
                    } else if (window.location.hash.length <= 2) {
                        window.location.hash = '#/';
                    } else {
                        window.location.href = 'https://dmfirst1.com/#/';
                    }
                } catch(e) {
                    window.location.href = 'https://dmfirst1.com/#/';
                }
            })();
        """.trimIndent()
    }

    /**
     * Script to navigate to Win Go game from Home, select WinGo 1Min tab, and open Game History.
     */
    fun generateNavigateToWinGo1MinScript(): String {
        return """
            (function() {
                function notify(msg) {
                    if (window.AndroidBridge && window.AndroidBridge.onStatusUpdate) {
                        window.AndroidBridge.onStatusUpdate(msg);
                    }
                }

                function clickWinGoItem() {
                    const allEls = Array.from(document.querySelectorAll('*'));
                    for (const el of allEls) {
                        const txt = (el.innerText || el.textContent || '').trim();
                        if (/^win\s*go$/i.test(txt) || txt.toLowerCase() === 'wingo' || txt.includes('Win Go') || txt.includes('WinGo')) {
                            // Find clickable parent or itself
                            const clickable = el.closest('div, a, button, li') || el;
                            clickable.click();
                            notify('Clicked Win Go game item');
                            return true;
                        }
                    }
                    return false;
                }

                function clickWinGo1MinTab() {
                    const allEls = Array.from(document.querySelectorAll('*'));
                    for (const el of allEls) {
                        const txt = (el.innerText || el.textContent || '').trim();
                        if (/^win\s*go\s*1\s*min$/i.test(txt) || /^1\s*min$/i.test(txt) || /^1min$/i.test(txt) || txt === 'Win Go 1Min' || txt === '1Min') {
                            const clickable = el.closest('.van-tab, div, button, a') || el;
                            clickable.click();
                            notify('Selected WinGo 1 min tab');
                            return true;
                        }
                    }
                    return false;
                }

                function clickGameHistoryTab() {
                    const allEls = Array.from(document.querySelectorAll('*'));
                    for (const el of allEls) {
                        const txt = (el.innerText || el.textContent || '').trim();
                        if (/^game\s*history$/i.test(txt) || txt.includes('Game history') || txt.includes('Game History')) {
                            const clickable = el.closest('.van-tab, div, button, a') || el;
                            clickable.click();
                            notify('Selected Game history tab');
                            return true;
                        }
                    }
                    return false;
                }

                notify('Navigating to Win Go...');
                const inWinGo = window.location.hash.toLowerCase().includes('wingo');
                if (!inWinGo) {
                    if (!clickWinGoItem()) {
                        // Direct hash fallback
                        window.location.hash = '#/lottery/winGo';
                    }
                }

                // Check and click 1 min tab after short delay
                setTimeout(() => {
                    clickWinGo1MinTab();
                    setTimeout(() => {
                        clickGameHistoryTab();
                    }, 800);
                }, 1000);
            })();
        """.trimIndent()
    }

    /**
     * Script to scrape 50 pages of Game History (10 records each) and send them to Room DB via AndroidBridge.
     */
    fun generate50PagesScraperScript(): String {
        return """
            (function() {
                window.__crawlerCancelled = false;

                function notify(msg) {
                    if (window.AndroidBridge && window.AndroidBridge.onStatusUpdate) {
                        window.AndroidBridge.onStatusUpdate(msg);
                    }
                }

                function getColorFromNumber(num) {
                    if (num === 0) return 'Red + Violet';
                    if (num === 5) return 'Green + Violet';
                    if ([1, 3, 7, 9].includes(num)) return 'Green';
                    return 'Red';
                }

                function getBigSmallFromNumber(num) {
                    return num >= 5 ? 'Big' : 'Small';
                }

                function extractCurrentPageRecords(pageNum) {
                    const records = [];
                    
                    // Look for table rows or record flex items
                    // Method 1: Check standard table tbody tr
                    let rows = Array.from(document.querySelectorAll('table tbody tr, .van-table tbody tr'));
                    
                    // Method 2: If no table rows, look for record container items
                    if (rows.length === 0) {
                        const historySection = Array.from(document.querySelectorAll('*')).find(el => {
                            const t = (el.innerText || '').trim();
                            return t.includes('Game history') || t.includes('Game History');
                        });
                        
                        const root = historySection ? (historySection.closest('div[class*="game" i]') || historySection.parentElement) : document.body;
                        
                        // Select candidates that have digits / columns
                        const candidates = Array.from(root.querySelectorAll('div[class*="row" i], div[class*="item" i], div[class*="list" i] > div, tr'));
                        rows = candidates.filter(c => {
                            const txt = c.innerText || '';
                            // must contain period like digits (e.g. 5+ digits) and Big/Small or 0-9
                            return /\d{5,}/.test(txt) && (/\b(big|small|大|小)\b/i.test(txt) || /[0-9]/.test(txt));
                        });
                    }

                    for (const row of rows) {
                        try {
                            const text = (row.innerText || row.textContent || '').trim();
                            if (!text || text.includes('Period') || text.includes('Number')) continue;

                            let period = '';
                            let number = -1;
                            let bigSmall = '';

                            // Method A: Check child columns directly (standard 4-column table: Period | Number | Big Small | Color)
                            const cols = Array.from(row.children);
                            if (cols.length >= 3) {
                                const col0Text = (cols[0].innerText || '').trim();
                                const col1Text = (cols[1].innerText || '').trim();
                                const col2Text = (cols[2].innerText || '').trim();

                                const pMatch = col0Text.match(/\d{14,18}/) || col0Text.match(/\d{10,}/);
                                if (pMatch && /^[0-9]$/.test(col1Text)) {
                                    period = pMatch[0];
                                    number = parseInt(col1Text, 10);
                                    if (/big|大/i.test(col2Text)) bigSmall = 'Big';
                                    else if (/small|小/i.test(col2Text)) bigSmall = 'Small';
                                }
                            }

                            // Method B: Regex fallback across whole row text
                            if (!period) {
                                const periodMatch = text.match(/\d{14,18}/) || text.match(/\d{10,}/) || text.match(/\d{6,}/);
                                if (!periodMatch) continue;
                                period = periodMatch[0];
                            }

                            if (number === -1) {
                                const numElem = row.querySelector('.van-circle, [class*="num" i], [class*="ball" i], span[class*="number" i]');
                                if (numElem) {
                                    const n = parseInt((numElem.innerText || '').trim(), 10);
                                    if (!isNaN(n) && n >= 0 && n <= 9) number = n;
                                }
                            }

                            if (number === -1) {
                                const parts = text.split(/\s+/).map(p => p.trim()).filter(Boolean);
                                for (const p of parts) {
                                    if (/^[0-9]$/.test(p)) {
                                        number = parseInt(p, 10);
                                        break;
                                    }
                                }
                            }

                            if (!bigSmall) {
                                if (/big|大/i.test(text)) bigSmall = 'Big';
                                else if (/small|小/i.test(text)) bigSmall = 'Small';
                                else if (number !== -1) bigSmall = getBigSmallFromNumber(number);
                                else bigSmall = 'Big';
                            }

                            if (number === -1) {
                                number = bigSmall === 'Big' ? 7 : 3;
                            }

                            let color = getColorFromNumber(number);

                            records.push({
                                period: period,
                                number: number,
                                bigSmall: bigSmall,
                                color: color,
                                page: pageNum,
                                gameType: 'WinGo 1Min'
                            });
                        } catch(err) {
                            console.error('Error parsing row:', err);
                        }
                    }

                    return records;
                }

                function findNextPageButton() {
                    // Check standard pagination classes in Vant or mobile CSS
                    const nextSelectors = [
                        '.van-pagination__next:not(.van-pagination__item--disabled)',
                        '.van-pagination__item--next',
                        'button[class*="next" i]:not([disabled])',
                        'li[class*="next" i]:not([class*="disabled" i])',
                        '[aria-label="Next"]:not([disabled])',
                        'i[class*="arrow-right" i]',
                        '.van-icon-arrow',
                        'div[class*="pagination" i] button:last-child'
                    ];

                    for (const sel of nextSelectors) {
                        const el = document.querySelector(sel);
                        if (el && !el.classList.contains('van-pagination__item--disabled') && !el.hasAttribute('disabled')) {
                            return el;
                        }
                    }

                    // Fallback: button or element with text ">"
                    const allBtns = Array.from(document.querySelectorAll('button, div[role="button"], span, div'));
                    for (const b of allBtns) {
                        const t = (b.innerText || '').trim();
                        if (t === '>' || t === '下一页' || t.toLowerCase() === 'next') {
                            if (!b.classList.contains('disabled') && !b.hasAttribute('disabled')) {
                                return b;
                            }
                        }
                    }

                    return null;
                }

                let currentPage = 1;
                const maxPages = 50;
                let totalStoredCount = 0;

                notify('Starting 50-page historical sync...');

                function processPage() {
                    if (window.__crawlerCancelled) {
                        notify('Crawler stopped.');
                        return;
                    }

                    const records = extractCurrentPageRecords(currentPage);
                    if (records.length > 0) {
                        totalStoredCount += records.length;
                        if (window.AndroidBridge && window.AndroidBridge.onBatchRecords) {
                            window.AndroidBridge.onBatchRecords(JSON.stringify(records), currentPage, maxPages);
                        }
                        if (window.AndroidBridge && window.AndroidBridge.onScrapeProgress) {
                            window.AndroidBridge.onScrapeProgress(currentPage, maxPages, totalStoredCount);
                        }
                    }

                    if (currentPage >= maxPages) {
                        notify('Completed syncing all ' + maxPages + ' pages! (' + totalStoredCount + ' records)');
                        if (window.AndroidBridge && window.AndroidBridge.onScrapeFinished) {
                            window.AndroidBridge.onScrapeFinished(totalStoredCount);
                        }
                        // Start real-time watching after deep sync
                        startRealtimeWatcher();
                        return;
                    }

                    const nextBtn = findNextPageButton();
                    if (nextBtn) {
                        currentPage++;
                        nextBtn.click();
                        notify('Scraping Page ' + currentPage + '/' + maxPages + ' (' + totalStoredCount + ' saved)...');
                        setTimeout(processPage, 750);
                    } else {
                        notify('Reached end of available pages at Page ' + currentPage + ' (' + totalStoredCount + ' records).');
                        if (window.AndroidBridge && window.AndroidBridge.onScrapeFinished) {
                            window.AndroidBridge.onScrapeFinished(totalStoredCount);
                        }
                        startRealtimeWatcher();
                    }
                }

                processPage();

                function startRealtimeWatcher() {
                    if (window.__realtimeWatcherTimer) {
                        clearInterval(window.__realtimeWatcherTimer);
                    }
                    notify('Real-time sync active: Monitoring new periods every 3s...');
                    let lastSeenPeriod = '';

                    window.__realtimeWatcherTimer = setInterval(() => {
                        try {
                            const latestRecords = extractCurrentPageRecords(1);
                            if (latestRecords.length > 0) {
                                const top = latestRecords[0];
                                if (top.period !== lastSeenPeriod) {
                                    lastSeenPeriod = top.period;
                                    if (window.AndroidBridge && window.AndroidBridge.onNewGameRecord) {
                                        window.AndroidBridge.onNewGameRecord(JSON.stringify(top));
                                    }
                                    notify('New result: ' + top.period + ' -> Number ' + top.number + ' (' + top.bigSmall + ', ' + top.color + ')');
                                }
                            }
                        } catch(e) {
                            console.error('Watcher error:', e);
                        }
                    }, 3500);
                }
            })();
        """.trimIndent()
    }

    /**
     * Stable, lightweight real-time watcher: 2-second polling with re-entrancy guard to prevent Chromium renderer overload.
     */
    fun generateRealtimeWatcherScript(): String {
        return """
            (function() {
                if (window.__realtimeWatcherTimer) {
                    clearInterval(window.__realtimeWatcherTimer);
                    window.__realtimeWatcherTimer = null;
                }
                if (window.__realtimeWatcherObserver) {
                    try { window.__realtimeWatcherObserver.disconnect(); } catch(_) {}
                    window.__realtimeWatcherObserver = null;
                }

                function notify(msg) {
                    if (window.AndroidBridge && window.AndroidBridge.onStatusUpdate) {
                        window.AndroidBridge.onStatusUpdate(msg);
                    }
                }

                function getColorFromNumber(num) {
                    if (num === 0) return 'Red + Violet';
                    if (num === 5) return 'Green + Violet';
                    if ([1, 3, 7, 9].includes(num)) return 'Green';
                    return 'Red';
                }

                function getBigSmallFromNumber(num) {
                    return num >= 5 ? 'Big' : 'Small';
                }

                notify('Live real-time watcher active (<5s sync)...');
                const seenPeriods = new Set();
                let isScanning = false;

                function scanAndReportLatestRecords() {
                    if (isScanning) return;
                    isScanning = true;
                    try {
                        let rows = Array.from(document.querySelectorAll('table tbody tr, .van-table tbody tr'));
                        if (rows.length === 0) {
                            const candidates = Array.from(document.querySelectorAll('div[class*="row" i], div[class*="item" i], div[class*="list" i] > div, tr'));
                            rows = candidates.filter(c => {
                                const txt = c.innerText || '';
                                return /\d{10,}/.test(txt);
                            });
                        }

                        if (rows.length === 0) return;

                        // Check all visible rows (up to 10 rows on page 1)
                        const checkRows = rows.slice(0, 10);
                        for (const row of checkRows) {
                            const text = (row.innerText || row.textContent || '').trim();
                            if (!text || text.includes('Period') || text.includes('Number')) continue;

                            let period = '';
                            let number = -1;
                            let bigSmall = '';

                            const cols = Array.from(row.children);
                            if (cols.length >= 3) {
                                const col0 = (cols[0].innerText || cols[0].textContent || '').trim();
                                const col1 = (cols[1].innerText || cols[1].textContent || '').trim();
                                const col2 = (cols[2].innerText || cols[2].textContent || '').trim();

                                const pMatch = col0.match(/202\d{11,15}/) || col0.match(/\d{14,18}/) || col0.match(/\d{10,}/);
                                const numMatch = col1.match(/\b([0-9])\b/) || col1.match(/([0-9])/);
                                if (pMatch && numMatch) {
                                    period = pMatch[0];
                                    number = parseInt(numMatch[1], 10);
                                    if (/big|大/i.test(col2)) bigSmall = 'Big';
                                    else if (/small|小/i.test(col2)) bigSmall = 'Small';
                                }
                            }

                            if (!period) {
                                const pMatch = text.match(/202\d{11,15}/) || text.match(/\d{14,18}/) || text.match(/\d{10,}/);
                                if (!pMatch) continue;
                                period = pMatch[0];
                            }

                            if (number === -1) {
                                const numElem = row.querySelector('.van-circle, [class*="num" i], [class*="ball" i], span[class*="number" i], b, strong');
                                if (numElem) {
                                    const nMatch = (numElem.innerText || numElem.textContent || '').trim().match(/[0-9]/);
                                    if (nMatch) number = parseInt(nMatch[0], 10);
                                }
                            }

                            if (number === -1) {
                                const cleanTxt = text.replace(period, '');
                                const nMatch = cleanTxt.match(/\b([0-9])\b/);
                                if (nMatch) {
                                    number = parseInt(nMatch[1], 10);
                                }
                            }

                            if (number === -1) continue;

                            if (!bigSmall) {
                                if (/big|大/i.test(text)) bigSmall = 'Big';
                                else if (/small|小/i.test(text)) bigSmall = 'Small';
                                else bigSmall = getBigSmallFromNumber(number);
                            }

                            const color = getColorFromNumber(number);

                            if (!seenPeriods.has(period)) {
                                seenPeriods.add(period);
                                const record = {
                                    period: period,
                                    number: number,
                                    bigSmall: bigSmall,
                                    color: color,
                                    page: 1,
                                    gameType: 'WinGo 1Min',
                                    timestamp: Date.now()
                                };

                                if (window.AndroidBridge && window.AndroidBridge.onNewGameRecord) {
                                    window.AndroidBridge.onNewGameRecord(JSON.stringify(record));
                                }
                                notify('LIVE SYNC: Period ' + period + ' -> Number ' + number + ' (' + bigSmall + ', ' + color + ')');
                            }
                        }
                    } catch(e) {
                        console.error('scanAndReport error:', e);
                    } finally {
                        isScanning = false;
                    }
                }

                // Initial scan
                scanAndReportLatestRecords();

                // Safe, throttled 2000ms periodic scan (lightweight and responsive)
                window.__realtimeWatcherTimer = setInterval(scanAndReportLatestRecords, 2000);
            })();
        """.trimIndent()
    }

    fun generateStopCrawlerScript(): String {
        return """
            (function() {
                window.__crawlerCancelled = true;
                if (window.__realtimeWatcherTimer) {
                    clearInterval(window.__realtimeWatcherTimer);
                    window.__realtimeWatcherTimer = null;
                }
                if (window.AndroidBridge && window.AndroidBridge.onStatusUpdate) {
                    window.AndroidBridge.onStatusUpdate('Crawler and real-time watcher paused.');
                }
            })();
        """.trimIndent()
    }
}
