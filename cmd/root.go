package cmd

import (
	"fmt"
	"github.com/spf13/cobra"
	"log"
)

var (
	indexPath string
	root = &cobra.Command{
		Use:   "bobolink",
		Short: "dump links, search them later",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println(cmd.Help())
		},
	}
)

func Execute() {
	if err := root.Execute(); err != nil {
		log.Fatal(err)
	}
}

func init() {
	root.PersistentFlags().StringVarP(&indexPath, "index-path", "i", "", "path to bobolink index")
}
