# 🎨 mynxx fhoto editir - Edição de Fotos para Android

Um aplicativo nativo para Android de edição profissional de fotos no estilo **Photoshop**, adaptado com interface moderna, rápida e otimizada para telas sensíveis ao toque de smartphones.

Desenvolvido em **Kotlin** com suporte total a **Android 8.0 até Android 16 (API 26 a 35)**.

---

## 📱 Recursos do Aplicativo

### 1. 📷 Captura na Hora ou da Galeria
- **Câmera Direta**: Tira fotos em alta resolução usando a câmera nativa do aparelho via `FileProvider`.
- **Galeria Integrada**: Escolha qualquer imagem da sua galeria de fotos via `Photo Picker` do Android.
- **Prevenção de OOM (Out of Memory)**: Decodificação com amostragem inteligente (`inSampleSize`) para processar imagens gigantes de câmeras modernas (50MP+) sem travar o celular.

### 2. 🖌️ Pincel & Borracha (Drawing Canvas)
- Desenho suave com curvas de Bézier em tempo real.
- **Paleta de Cores**: Branco, Preto, Vermelho, Laranja, Amarelo, Verde, Azul, Roxo.
- **Espessura Ajustável**: Controle deslizante com resposta imediata.
- **Borracha com Mistura Limpa**: Apague traços específicos com precisão.

### 3. 🎨 Filtros Artísticos & Ajustes de Cor
- **Presets Instantâneos**:
  - `Original`
  - `Preto & Branco (P&B)`
  - `Sépia Retrô`
  - `Vintage`
  - `Inverter Cores (Negativo)`
  - `Tom Quente (Warm)`
  - `Tom Frio (Cool)`
- **Controles Manuais Deslizantes**:
  - **Brilho** (-100 a +100)
  - **Contraste** (0.5x a 1.5x)
  - **Saturação** (0.0x a 2.0x)
- Renderização via hardware com `ColorMatrix` a 60 FPS.

### 4. 🔄 Rotação & Texto
- **Girar 90°**: Gire a imagem no sentido horário com um toque.
- **Texto sobre a Imagem**: Adicione títulos, legendas ou marcas d'água e arraste com o dedo para posicionar livremente na foto.

### 5. ⏪ Histórico Desfazer / Refazer (Undo / Redo)
- Pilha de ações completas para desfazer e refazer traços e adições de texto a qualquer momento.

### 6. 💾 Salvar na Galeria
- Exporta a imagem final renderizada em alta definição diretamente para a pasta pública `Pictures/PhotoshopMobile`.
- Totalmente compatível com **Scoped Storage** do Android 10 ao 16.

---

## 🛠️ Estrutura do Código

```text
photoshop-android/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Permissões de câmera e FileProvider
│   │   ├── java/com/mynxx/photoshop/
│   │   │   ├── MainActivity.kt          # Controlador da UI, ferramentas e câmera
│   │   │   ├── ui/
│   │   │   │   └── PhotoEditorView.kt   # Canvas interativo com gestos, traços e texto
│   │   │   ├── filters/
│   │   │   │   └── FilterManager.kt     # Manipulação de ColorMatrix e presets
│   │   │   └── utils/
│   │   │       └── ImageUtils.kt        # Decodificação segura, rotação e MediaStore
│   │   └── res/
│   │       ├── layout/activity_main.xml # Layout dark theme estilo Photoshop
│   │       ├── values/colors.xml        # Paleta escura (#181818, azul #0078D4)
│   │       ├── values/strings.xml       # Strings em português
│   │       └── xml/file_paths.xml       # Configuração de caminhos do FileProvider
│   └── build.gradle.kts                 # Configuração do módulo Android
├── build.gradle.kts                     # Gradle raiz
├── settings.gradle.kts                  # Configuração de repositórios e módulos
└── README.md
```

---

## 🚀 Como Abrir e Compilar

### No Android Studio:
1. Abra o **Android Studio**.
2. Clique em **File > Open** e selecione a pasta `photoshop-android`.
3. Aguarde a sincronização do Gradle.
4. Conecte seu celular Android via cabo USB (com Depuração USB ativada) ou inicie um emulador.
5. Clique no botão verde **Run (▶)** para instalar e rodar o app!

### Via Linha de Comando (Gradle):
```bash
# Compilar o APK de desenvolvimento
./gradlew assembleDebug

# O APK gerado estará em:
# app/build/outputs/apk/debug/app-debug.apk
```
