package main

import (
	"github.com/jtanza/bobolink/cmd"
	"os"
)

func main() {
	cli := cmd.NewCli([]cmd.Command{
		cmd.NewAddCommand(),
	})
	cli.Parse(os.Args)
}