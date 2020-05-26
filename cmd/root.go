package cmd

import (
	"fmt"
	"github.com/spf13/cobra"
	"os"
)

var IndexPath string

var rootCmd = &cobra.Command{
	Use:   "bobolink",
	Short: "dump links, search them later",
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Println(cmd.Help())
	},
}

func Execute() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}

func init() {
	rootCmd.PersistentFlags().StringVarP(&IndexPath, "index-path", "i", "", "return all links")
}
