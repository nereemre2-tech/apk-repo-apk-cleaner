const views = {
  select: document.querySelector('#select-view'),
  config: document.querySelector('#config-view'),
  work: document.querySelector('#work-view'),
  result: document.querySelector('#result-view'),
};
const steps = [...document.querySelectorAll('.steps div')];
const fileInput = document.querySelector('#package-file');
const fileName = document.querySelector('#file-name');
const fileDetail = document.querySelector('#file-detail');
const bar = document.querySelector('#bar');
const percentage = document.querySelector('#percentage');
const phase = document.querySelector('#phase');
const log = document.querySelector('#log');

function show(name, active) {
  Object.entries(views).forEach(([key, view]) => view.classList.toggle('hidden', key !== name));
  steps.forEach((item, index) => item.classList.toggle('active', index < active));
}

function packagePicked(file) {
  if (!file) return;
  const extension = file.name.split('.').pop().toLowerCase();
  if (!['apk', 'apks', 'apkm', 'xapk'].includes(extension)) {
    window.alert('APK, APKS, APKM veya XAPK uzantılı bir dosya seçin.');
    return;
  }
  fileName.textContent = file.name;
  fileDetail.textContent = `${(file.size / 1024 / 1024).toFixed(1)} MB · Yerel önizleme`;
  show('config', 2);
}

document.querySelector('#browse').addEventListener('click', (event) => { event.preventDefault(); fileInput.click(); });
fileInput.addEventListener('change', () => packagePicked(fileInput.files[0]));
document.querySelectorAll('.operation').forEach((button) => button.addEventListener('click', () => {
  document.querySelectorAll('.operation').forEach((item) => { item.classList.remove('selected'); item.querySelector('i').textContent = '○'; });
  button.classList.add('selected'); button.querySelector('i').textContent = '●';
}));
document.querySelectorAll('.profiles button').forEach((button) => button.addEventListener('click', () => { document.querySelectorAll('.profiles button').forEach((item) => item.classList.remove('selected')); button.classList.add('selected'); }));
document.querySelector('#start').addEventListener('click', () => {
  show('work', 3);
  const events = [
    [5, 'Çalışma alanı hazırlanıyor'], [19, 'Paket içeriği ve DEX dosyaları taranıyor'], [38, 'classes.dex için DEX yaması tamamlandı'], [57, 'İkinci DEX dosyası denetleniyor'], [72, 'Manifest reklam izinleri ve metadata kayıtları denetleniyor'], [84, 'Değişiklikler yeni APK paketine yazılıyor'], [94, 'Çıktı APK’sı yerel sertifikayla imzalanıyor'], [100, 'İşlem tamamlandı'],
  ];
  log.textContent = '✓ Orijinal paket korunuyor\n● Yerel motor için ayrı çalışma süreci başlatıldı';
  events.forEach(([value, text], index) => setTimeout(() => {
    percentage.textContent = `${value}%`; phase.textContent = text; bar.style.width = `${value}%`; log.textContent += `\n● ${text}`; log.scrollTop = log.scrollHeight;
    if (value === 100) setTimeout(() => show('result', 4), 650);
  }, index * 800));
});
document.querySelector('#again').addEventListener('click', () => { fileInput.value = ''; show('select', 1); });
