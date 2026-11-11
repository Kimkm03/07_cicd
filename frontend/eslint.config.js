import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist', 'src/components/AccoundCard2.jsx']), // 검사에서 제외할 경로. AccoundCard2.jsx 는 일부러 틀리게 만들어 둔 lint 연습 파일이라 고치지 않고 제외한다
  {
    files: ['**/*.{js,jsx}'], // 테스트를 실행할 경로
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
  },
])
