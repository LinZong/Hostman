package moe.nemesiss.hostman.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import moe.nemesiss.hostman.boost.EasyProcess
import moe.nemesiss.hostman.databinding.ExpertPostmortemActionPanelBinding
import moe.nemesiss.hostman.model.viewmodel.PostmortemViewModel


class PostmortemExpertActionPanelFragment : Fragment() {


    private lateinit var binding: ExpertPostmortemActionPanelBinding

    private lateinit var viewModel: PostmortemViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        this.binding = ExpertPostmortemActionPanelBinding.inflate(inflater, container, false)
        return this.binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.stacktraceText.text = viewModel.stackTrace.value
        binding.restartApp.setOnClickListener {
            context?.let { ctx ->
                EasyProcess.restartApp(ctx)
            }
        }
    }

    fun prepare(viewModel: PostmortemViewModel) {
        this.viewModel = viewModel
    }
}