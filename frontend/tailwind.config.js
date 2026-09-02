/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        grass: {
          50: '#f1faf0', 100: '#e1f5de', 200: '#c2ebc4', 300: '#93dc9b',
          400: '#5fc673', 500: '#2fae52', 600: '#188043', 700: '#146536',
          800: '#114d2a', 900: '#0e3b22'
        },
        sprout: { DEFAULT: '#a3d93f', soft: '#eaf7cf' },
        sun: { DEFAULT: '#f7b733', soft: '#fff4d6', ink: '#b45309' },
        sky: { DEFAULT: '#35a7d7', soft: '#ddf1fa', ink: '#0b7285' },
        berry: { DEFAULT: '#e8604c', soft: '#fde6e1', ink: '#c0392b' },
        plum: { DEFAULT: '#8b6be0', soft: '#ece5fd', ink: '#5b3fb0' },
        ink: { strong: '#12301c', body: '#33493c', muted: '#5a6b60' }
      },
      fontFamily: {
        sans: ['PingFang SC', 'HarmonyOS Sans SC', 'Source Han Sans SC', 'Microsoft YaHei',
          '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif']
      },
      borderRadius: { card: '20px', pill: '999px' },
      boxShadow: {
        soft: '0 2px 10px rgba(18, 60, 34, 0.06)',
        lift: '0 10px 26px rgba(18, 60, 34, 0.10)',
        green: '0 10px 24px rgba(24, 128, 67, 0.28)'
      },
      keyframes: {
        rise: { from: { opacity: '0', transform: 'translate3d(0,16px,0)' }, to: { opacity: '1', transform: 'none' } },
        pop: { '0%': { opacity: '0', transform: 'scale(.86)' }, '70%': { transform: 'scale(1.04)' }, '100%': { opacity: '1', transform: 'scale(1)' } },
        float: { '0%,100%': { transform: 'translateY(0)' }, '50%': { transform: 'translateY(-7px)' } },
        sway: { '0%,100%': { transform: 'rotate(-4deg)' }, '50%': { transform: 'rotate(4deg)' } }
      },
      animation: {
        rise: 'rise .32s cubic-bezier(.22,.8,.24,1) both',
        pop: 'pop .32s cubic-bezier(.34,1.46,.64,1) both',
        float: 'float 4.6s ease-in-out infinite',
        sway: 'sway 3.4s ease-in-out infinite'
      }
    }
  },
  plugins: [],
}
