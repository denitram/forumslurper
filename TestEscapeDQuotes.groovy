def escapeDQuotes(string) {
	return string.replaceAll('"','""')
}

foo = '''
"Well", he said, "it's not good."
'''

assert escapeDQuotes(foo) == '''
""Well"", he said, ""it's not good.""
'''

