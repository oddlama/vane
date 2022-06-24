/** @type {import('tailwindcss').Config} */
const defaultTheme = require('tailwindcss/defaultTheme')
module.exports = {
	content: ["../docs/index.html"],
	theme: {
		extend: {},
	},
	theme: {
		extend: {
			fontFamily: {
				sans: ['Inter var', ...defaultTheme.fontFamily.sans],
			},
		},
	},
	plugins: [
		require('@tailwindcss/typography'),
		require('flowbite/plugin')
	],
}
