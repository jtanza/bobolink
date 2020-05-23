package cmd

import (
	"github.com/jtanza/bobolink/internal/search"
	"github.com/spf13/cobra"
	"log"
)

func init() {
	var addCmd = &cobra.Command{
		Use:   "add",
		Short: "adds urls to store",
		Run: func(cmd *cobra.Command, args []string) {
			if _, err := search.AddResources(args); err != nil {
				log.Fatal(err)
			}
		},
	}
	rootCmd.AddCommand(addCmd)
}
