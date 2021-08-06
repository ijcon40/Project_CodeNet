function counter() {

}

function count(c) {
    if (c.inc() < 5) {
	count(c)
    }
}

var c = new counter()
c.total = 0
c.inc = function inc() {
    return this.total++
}
	
count(c)
