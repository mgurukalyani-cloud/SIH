# Telugu Offline Neural Weights Directory

Place the following INT8 quantized ONNX models in this folder for production on-device neural execution:

1. `tokens.txt` — Telugu BPE vocabulary dictionary (Kathbath Indic)
2. `conformer_ctc_int8.onnx` — AI4Bharat IndicASR Telugu Conformer INT8 model (52.4 MB)
3. `vits_telugu_int8.onnx` — AI4Bharat IndicTTS Telugu VITS model (42.0 MB)

When files are present, `SherpaOnnxSTTAdapter` automatically binds them into memory. If not present, iTantra seamlessly runs the authentic offline demonstration and simulation pipeline, logging the status cleanly in accordance with system integrity standards.
