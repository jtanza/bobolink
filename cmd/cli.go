package cmd

import (
	"fmt"
	"log"
	"os"
)

type CLI struct {
	Commands []Command
	Lookup map[string]Command
}

func NewCLI(commands []Command) *CLI {
	m := make(map[string]Command)
	for _, c := range commands {
		m[c.Name()] = c
	}
	return &CLI{
		Commands: commands,
		Lookup: m,
	}
}

type Command interface {
	Run(args []string) error
	Name() string
	Help() string
	Usage() string
}

func (cli CLI) Exec(args []string) {
	if len(args) == 0 {
		fmt.Println(cli)
		os.Exit(0)
	}

	name := args[0]
	c, ok := cli.Lookup[name]
	if !ok {
		log.Fatal(cli)
	}

	if len(args) > 1 && args[1] == "--help" {
		fmt.Println(c)
		os.Exit(1)
	}

	if err := c.Run(args[1:]); err != nil {
		fmt.Printf("error %v\n%s", err, c.Usage())
		os.Exit(1)
	}
}

func (cli CLI) String() string {
	// TODO format all output
	return "usage: of cli is"
}