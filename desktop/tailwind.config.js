/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: ['./src/renderer/index.html', './src/renderer/src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        surface: {
          DEFAULT: '#fbf9f9',
          dim: '#dbdad9',
          bright: '#fbf9f9',
          container: {
            DEFAULT: '#efeded',
            low: '#f5f3f3',
            high: '#e9e8e7',
            highest: '#e3e2e2'
          }
        },
        'on-surface': {
          DEFAULT: '#1b1c1c',
          variant: '#4c4546'
        },
        'inverse-surface': '#303031',
        'inverse-on-surface': '#f2f0f0',
        outline: {
          DEFAULT: '#7e7576',
          variant: '#cfc4c5'
        }
      },
      fontFamily: {
        body: ['Plus Jakarta Sans', 'sans-serif']
      },
      dropShadow: {
        'center': '0 0 6px rgba(0,0,0,0.12)',
        'center-dark': '0 0 8px rgba(255,255,255,0.15)'
      },
      borderRadius: {
        sm: '0.5rem',
        DEFAULT: '1rem',
        md: '1.5rem',
        lg: '0.75rem',
        xl: '3rem'
      }
    }
  },
  plugins: []
}
