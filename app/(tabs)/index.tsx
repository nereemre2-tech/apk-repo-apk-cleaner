import * as DocumentPicker from "expo-document-picker";
import * as Sharing from "expo-sharing";
import { useMemo, useState } from "react";
import { ActivityIndicator, Alert, Pressable, ScrollView, Switch, Text, View } from "react-native";

import { ScreenContainer } from "@/components/screen-container";
import { analyzePackage, formatSize, processPackage, type Analysis, type ProcessingResult } from "@/lib/apk-cleaner";
import { addHistory } from "@/lib/job-history";

type Profile = "safe" | "balanced" | "deep";
const profileOptions: Array<{ id: Profile; title: string; text: string }> = [
  { id: "safe", title: "Güvenli", text: "Kesin reklam çağrılarını etkisizleştirir." },
  { id: "balanced", title: "Dengeli", text: "DEX yamasına ek olarak manifest adaylarını temizler." },
  { id: "deep", title: "Kapsamlı", text: "Doğrulanmış asset ve kütüphane kalıntılarını da kaldırır." },
];

const supported = (name: string) => ["apk", "apks", "apkm", "xapk"].includes(name.split(".").pop()?.toLowerCase() ?? "");

export default function CleanerScreen() {
  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [sourceName, setSourceName] = useState("");
  const [profile, setProfile] = useState<Profile>("balanced");
  const [patchAds, setPatchAds] = useState(true);
  const [stripDebug, setStripDebug] = useState(false);
  const [busy, setBusy] = useState<"analyze" | "process" | null>(null);
  const [result, setResult] = useState<ProcessingResult | null>(null);
  const canProcess = Boolean(analysis && !busy && (!patchAds || analysis.networkCount > 0));
  const actionLabel = useMemo(() => {
    if (!analysis) return "Önce paket seçin";
    if (analysis.splitMerged && !patchAds && !stripDebug) return "Tek APK oluştur";
    return patchAds ? "Temizleme işlemini başlat" : "Paketi yeniden imzala";
  }, [analysis, patchAds, stripDebug]);

  async function pickPackage() {
    try {
      const response = await DocumentPicker.getDocumentAsync({ type: "*/*", copyToCacheDirectory: true });
      if (response.canceled) return;
      const item = response.assets[0];
      if (!supported(item.name)) {
        Alert.alert("Desteklenmeyen dosya", "APK, APKS, APKM veya XAPK uzantılı bir paket seçin.");
        return;
      }
      setBusy("analyze");
      setResult(null);
      const report = await analyzePackage(item.uri, item.name);
      setAnalysis(report);
      setSourceName(item.name);
      setPatchAds(report.networkCount > 0);
    } catch (error) {
      Alert.alert("Analiz tamamlanamadı", error instanceof Error ? error.message : "Seçilen dosya analiz edilemedi.");
    } finally {
      setBusy(null);
    }
  }

  async function startProcess() {
    if (!analysis) return;
    try {
      setBusy("process");
      const output = await processPackage({ jobId: analysis.jobId, profile, patchAds, stripDebug, sourceName });
      setResult(output);
      await addHistory(sourceName, output);
    } catch (error) {
      Alert.alert("İşlem tamamlanamadı", error instanceof Error ? error.message : "Yerel işlem motoru hata verdi.");
    } finally {
      setBusy(null);
    }
  }

  async function shareOutput() {
    if (!result) return;
    if (!(await Sharing.isAvailableAsync())) {
      Alert.alert("Paylaşım kullanılamıyor", "Bu cihazda yerel dosya paylaşımı kullanılamıyor.");
      return;
    }
    await Sharing.shareAsync(result.outputUri, {
      dialogTitle: "Oluşturulan APK’yı kaydet veya paylaş",
      mimeType: "application/vnd.android.package-archive",
    });
  }

  return (
    <ScreenContainer className="px-5" edges={["top", "left", "right"]}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.hero}>
          <Text style={styles.eyebrow}>YEREL MOTOR</Text>
          <Text style={styles.title}>APK Cleaner{"\n"}Manager</Text>
          <Text style={styles.subtitle}>Paketinizi cihazda analiz edin, izin verilen yerel işlemleri uygulayın ve sonucu doğrudan paylaşın.</Text>
        </View>

        <Pressable onPress={pickPackage} disabled={Boolean(busy)} style={({ pressed }) => [styles.fileCard, pressed && styles.pressed, busy && styles.disabled]}>
          <View style={styles.fileBadge}><Text style={styles.fileBadgeText}>APK</Text></View>
          <View style={styles.grow}>
            <Text style={styles.fileTitle}>{busy === "analyze" ? "Paket analiz ediliyor…" : analysis ? sourceName : "Paket seç"}</Text>
            <Text style={styles.fileSubtitle}>{analysis ? `${formatSize(analysis.size)} · ${analysis.dexCount} DEX dosyası` : "APK · APKS · APKM · XAPK"}</Text>
          </View>
          {busy === "analyze" ? <ActivityIndicator color="#38C7D5" /> : <Text style={styles.arrow}>›</Text>}
        </Pressable>

        {analysis && <>
          <Text style={styles.section}>Analiz özeti</Text>
          <View style={styles.summary}><View style={styles.stat}><Text style={styles.statValue}>{analysis.dexCount}</Text><Text style={styles.statLabel}>DEX</Text></View><View style={styles.stat}><Text style={styles.statValue}>{analysis.networkCount}</Text><Text style={styles.statLabel}>reklam ağı</Text></View><View style={styles.stat}><Text style={styles.statValue}>{analysis.splitMerged ? "Split" : "APK"}</Text><Text style={styles.statLabel}>paket türü</Text></View></View>
          {analysis.detections.length > 0 ? <View style={styles.card}>{analysis.detections.map((item) => <View key={item.id} style={styles.row}><Text style={styles.rowTitle}>{item.label}</Text><Text style={styles.count}>{item.references} referans</Text></View>)}</View> : <View style={styles.notice}><Text style={styles.noticeTitle}>Bilinen reklam ağı algılanmadı</Text><Text style={styles.noticeText}>İsterseniz DEX hata ayıklama verisini temizleyebilir veya split paketi tek APK’ya dönüştürebilirsiniz.</Text></View>}
          {analysis.splitMerged && <View style={styles.notice}><Text style={styles.noticeTitle}>Split paket bulundu</Text><Text style={styles.noticeText}>{analysis.modules.length} modül tek APK çıktısı için birleştirilecektir. İlk bağımsız Android sürümünde modül seçimi otomatik uygulanır.</Text></View>}

          <Text style={styles.section}>İşlem profili</Text>
          <View style={styles.card}>{profileOptions.map((item) => <Pressable key={item.id} onPress={() => setProfile(item.id)} style={({ pressed }) => [styles.profileRow, profile === item.id && styles.activeProfile, pressed && styles.pressed]}><View style={styles.grow}><Text style={styles.rowTitle}>{item.title}</Text><Text style={styles.optionText}>{item.text}</Text></View><Text style={styles.choice}>{profile === item.id ? "●" : "○"}</Text></Pressable>)}</View>
          <View style={styles.option}><View style={styles.grow}><Text style={styles.rowTitle}>Reklam yaması</Text><Text style={styles.optionText}>Bilinen ağ referanslarında güvenli DEX yaması uygular.</Text></View><Switch value={patchAds} onValueChange={setPatchAds} disabled={analysis.networkCount === 0} trackColor={{ false: "#AAB7C7", true: "#007E8A" }} /></View>
          <View style={styles.option}><View style={styles.grow}><Text style={styles.rowTitle}>DEX hata ayıklama verisi</Text><Text style={styles.optionText}>Kaynak, satır ve yerel değişken kayıtlarını temizler.</Text></View><Switch value={stripDebug} onValueChange={setStripDebug} trackColor={{ false: "#AAB7C7", true: "#007E8A" }} /></View>
          <Pressable onPress={startProcess} disabled={!canProcess} style={({ pressed }) => [styles.primary, pressed && styles.pressed, !canProcess && styles.disabled]}>{busy === "process" ? <ActivityIndicator color="#FFFFFF" /> : <Text style={styles.primaryText}>{actionLabel}</Text>}</Pressable>
          <Text style={styles.legal}>Yalnızca sahibi olduğunuz veya değiştirmeye yetkili olduğunuz paketleri işleyin. Oluşturulan çıktı, yerel sertifikayla yeniden imzalanır.</Text>
        </>}
        {result && <View style={styles.result}><Text style={styles.resultEyebrow}>İŞLEM TAMAMLANDI</Text><Text style={styles.resultName}>{result.outputName}</Text><Text style={styles.resultText}>{result.dexPatched} DEX · {result.manifestPatched} manifest · {result.removedFiles} dosya değişikliği</Text><Pressable onPress={shareOutput} style={({ pressed }) => [styles.share, pressed && styles.pressed]}><Text style={styles.shareText}>Kaydet / paylaş</Text></Pressable><Text style={styles.resultNote}>Not: XML reklam görünümü gizleme ve ZIP hizalama işlemleri bu bağımsız Android sürümünde uygulanmaz.</Text></View>}
      </ScrollView>
    </ScreenContainer>
  );
}

const styles: Record<string, any> = {
  content: { paddingTop: 18, paddingBottom: 32, gap: 14 }, hero: { gap: 9, paddingTop: 2, paddingBottom: 8 }, eyebrow: { alignSelf: "flex-start", backgroundColor: "#D7F4F7", borderRadius: 99, color: "#00646E", fontSize: 10, fontWeight: "800", letterSpacing: 1, overflow: "hidden", paddingHorizontal: 10, paddingVertical: 5 }, title: { color: "#14213D", fontSize: 34, fontWeight: "800", lineHeight: 39 }, subtitle: { color: "#5D6B82", fontSize: 15, lineHeight: 22 }, fileCard: { alignItems: "center", backgroundColor: "#14213D", borderRadius: 20, flexDirection: "row", gap: 13, padding: 16 }, fileBadge: { alignItems: "center", backgroundColor: "#38C7D5", borderRadius: 12, height: 48, justifyContent: "center", width: 48 }, fileBadgeText: { color: "#0B1220", fontSize: 12, fontWeight: "900" }, grow: { flex: 1, gap: 4 }, fileTitle: { color: "#FFFFFF", fontSize: 16, fontWeight: "700" }, fileSubtitle: { color: "#B8C4D6", fontSize: 12 }, arrow: { color: "#FFFFFF", fontSize: 30 }, section: { color: "#14213D", fontSize: 17, fontWeight: "800", marginTop: 8 }, summary: { backgroundColor: "#FFFFFF", borderColor: "#D7E0EA", borderRadius: 17, borderWidth: 1, flexDirection: "row", paddingVertical: 14 }, stat: { alignItems: "center", flex: 1, gap: 2 }, statValue: { color: "#14213D", fontSize: 17, fontWeight: "800" }, statLabel: { color: "#5D6B82", fontSize: 11 }, card: { backgroundColor: "#FFFFFF", borderColor: "#D7E0EA", borderRadius: 17, borderWidth: 1, overflow: "hidden" }, row: { alignItems: "center", borderBottomColor: "#E9EFF5", borderBottomWidth: 1, flexDirection: "row", justifyContent: "space-between", paddingHorizontal: 15, paddingVertical: 13 }, rowTitle: { color: "#14213D", fontSize: 14, fontWeight: "700" }, count: { color: "#007E8A", fontSize: 12, fontWeight: "700" }, notice: { backgroundColor: "#EAF6F7", borderColor: "#BDE4E8", borderRadius: 15, borderWidth: 1, gap: 4, padding: 14 }, noticeTitle: { color: "#075C65", fontSize: 13, fontWeight: "800" }, noticeText: { color: "#386A71", fontSize: 12, lineHeight: 18 }, profileRow: { alignItems: "center", borderBottomColor: "#E9EFF5", borderBottomWidth: 1, flexDirection: "row", gap: 12, padding: 14 }, activeProfile: { backgroundColor: "#EFFBFC" }, optionText: { color: "#5D6B82", fontSize: 11, lineHeight: 16 }, choice: { color: "#007E8A", fontSize: 20 }, option: { alignItems: "center", backgroundColor: "#FFFFFF", borderColor: "#D7E0EA", borderRadius: 17, borderWidth: 1, flexDirection: "row", gap: 12, padding: 14 }, primary: { alignItems: "center", backgroundColor: "#007E8A", borderRadius: 15, justifyContent: "center", minHeight: 52 }, primaryText: { color: "#FFFFFF", fontSize: 15, fontWeight: "800" }, legal: { color: "#69788F", fontSize: 11, lineHeight: 16, paddingHorizontal: 4, textAlign: "center" }, result: { backgroundColor: "#14213D", borderRadius: 19, gap: 7, padding: 17 }, resultEyebrow: { color: "#76D6DF", fontSize: 10, fontWeight: "800", letterSpacing: 1 }, resultName: { color: "#FFFFFF", fontSize: 17, fontWeight: "800" }, resultText: { color: "#B8C4D6", fontSize: 12 }, share: { alignItems: "center", backgroundColor: "#38C7D5", borderRadius: 12, justifyContent: "center", marginTop: 6, minHeight: 44 }, shareText: { color: "#0B1220", fontSize: 14, fontWeight: "800" }, resultNote: { color: "#AFC0D5", fontSize: 11, lineHeight: 16 }, pressed: { opacity: 0.76, transform: [{ scale: 0.985 }] }, disabled: { opacity: 0.5 },
};
