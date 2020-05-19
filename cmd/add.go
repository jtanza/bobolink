package cmd

import (
	"github.com/jtanza/bobolink/internal/search"
	"github.com/spf13/cobra"
)

func init() {
	var addCmd = &cobra.Command{
		Use:   "add",
		Short: "adds urls to store",
		Run: func(cmd *cobra.Command, args []string) {
			search.AddResources(args)
		},
	}
	rootCmd.AddCommand(addCmd)
}
