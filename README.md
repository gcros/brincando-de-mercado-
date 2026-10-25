# Brincando de Mercado 🛒

Aplicativo Android de mercado virtual para brincar de fazer compras usando saldo fictício. Nenhuma transação financeira real é realizada.

## Recursos
- Catálogo inicial de produtos com preços em reais.
- Saldo inicial fictício de R$ 500,00, salvo no aparelho.
- Carrinho com controles de quantidade e cálculo do total.
- Pagamento simulado por cartão ou dinheiro.
- Validação de saldo insuficiente e comprovante fictício.
- Botão para reiniciar a brincadeira.
- Workflow GitHub Actions para compilar APK de depuração.

## Como obter o APK
1. Abra a aba **Actions** deste repositório.
2. Se necessário, selecione **Build Android APK** e clique em **Run workflow**.
3. Aguarde o workflow terminar com sucesso.
4. Abra a execução concluída e baixe o artefato **BrincandoDeMercado-debug-apk**.
5. Extraia o ZIP e instale `app-debug.apk` no Android.

## Compilar localmente
Requer JDK 17 e Gradle 8.9:
```bash
gradle assembleDebug
```
O APK será criado em `app/build/outputs/apk/debug/app-debug.apk`.

## Observação
Este é um jogo educativo/simulador. Cartão e dinheiro são fictícios; não informe dados reais de cartão. O saldo e o carrinho são armazenados localmente no dispositivo.
